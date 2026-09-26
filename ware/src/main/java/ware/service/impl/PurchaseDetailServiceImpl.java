package ware.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import common.exception.BaseCodeEnum;
import common.exception.BaseException;
import common.exception.ValidationException;
import common.vo.PageVO;
import common.utils.R;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ware.costant.PurchaseDetailEnum;
import ware.dao.PurchaseDetailDao;
import ware.entity.PurchaseDetailEntity;
import ware.entity.WareInfoEntity;
import ware.feign.ProductFeignService;
import ware.service.PurchaseDetailService;
import ware.service.WareInfoService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;


import ware.vo.PurchaseDetailPageQuery;
/**
 * 采购需求单服务的默认实现，基于 MyBatis-Plus 的 {@code ServiceImpl} 读写 {@code wms_purchase_detail}。
 *
 * <p>状态与归属一律由本类写入，列表查询通过商品服务补齐 SKU 名称；
 * 字段校验集中在 {@link #validate(PurchaseDetailEntity)}，仓库与 SKU 的存在性在写入前各查一次。
 */
@Service("purchaseDetailService")
public class PurchaseDetailServiceImpl extends ServiceImpl<PurchaseDetailDao, PurchaseDetailEntity> implements PurchaseDetailService {
    private final ProductFeignService productFeignService;
    private final WareInfoService wareInfoService;

    /**
     * 由容器注入商品服务客户端与仓库服务构造。
     *
     * @param productFeignService 商品服务客户端，列表查询补 SKU 名称、建单前确认 SKU 存在
     * @param wareInfoService 仓库服务，列表查询补仓库名、建单前确认仓库存在
     */
    public PurchaseDetailServiceImpl(ProductFeignService productFeignService, WareInfoService wareInfoService) {
        this.productFeignService = productFeignService;
        this.wareInfoService = wareInfoService;
    }

    /** {@inheritDoc} */
    @Override
    public PageVO<PurchaseDetailEntity> queryPage(PurchaseDetailPageQuery query) {
        LambdaQueryWrapper<PurchaseDetailEntity> queryWrapper = new LambdaQueryWrapper<>();

        String key = query.getKey();

        String status = query.getStatus();
        if (status != null && !status.isEmpty()) {
            queryWrapper.eq(PurchaseDetailEntity::getStatus, status);
        }

        String wareId = query.getWareId();
        if (wareId != null && !wareId.isEmpty()) {
            queryWrapper.eq(PurchaseDetailEntity::getWareId, wareId);
        }

        // 按采购单查它下面的明细。"完成采购"要逐条填结果，前端必须能只取一张单的明细
        String purchaseId = query.getPurchaseId();
        if (purchaseId != null && !purchaseId.isEmpty()) {
            queryWrapper.eq(PurchaseDetailEntity::getPurchaseId, purchaseId);
        }

        IPage<PurchaseDetailEntity> page = this.page(
                query.toPage(),
                queryWrapper
        );

        // 1. 补仓库名：仓库被删过的话找不到，用空串兜住
        List<WareInfoEntity> list = wareInfoService.list();
        page.getRecords().forEach(item -> {
            item.setWareName(list.stream()
                    .filter(ware -> ware.getId().equals(item.getWareId()))
                    .findFirst()
                    .map(WareInfoEntity::getName)
                    .orElse(""));
            item.setAllowedActions(PurchaseDetailEnum.allowedActions(item.getStatus()));
        });

        // 2. 补 SKU 名称：只把当前页用到的 skuId 传给商品服务
        List<Long> skuIds = page.getRecords().stream()
                .map(PurchaseDetailEntity::getSkuId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (!skuIds.isEmpty()) {
            R<Map<Long, String>> r = productFeignService.getSkuNames(skuIds);

            if (r.getCode() != 0) {
                throw new BaseException(r.getCode(), "远程服务异常：" + r.getMsg());
            }

            Map<Long, String> skuNameMap = r.getData();

            page.getRecords().forEach(item -> item.setSkuName(skuNameMap.get(item.getSkuId())));
        }


        // 3. 关键字过滤。skuName 是远程查出来的，可能为 null，直接 contains 会 NPE
        if (key != null && !key.trim().isEmpty()) {
            page.getRecords().removeIf(item -> {
                String skuName = Objects.toString(item.getSkuName(), "");
                String skuId = Objects.toString(item.getSkuId(), "");
                return !skuName.contains(key) && !skuId.contains(key);
            });
        }

        return new PageVO<>(page.getTotal(), page.getRecords());
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public void saveDetail(PurchaseDetailEntity detail) {
        detail.setId(null);
        detail.setStatus(PurchaseDetailEnum.CREATED.getCode());
        detail.setPurchaseId(null);
        validate(detail);

        this.save(detail);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public void updateDetail(PurchaseDetailEntity detail) {
        if (detail.getId() == null) {
            throw new ValidationException("id", "请先选择要修改的采购需求单");
        }

        PurchaseDetailEntity existing = this.getById(detail.getId());
        if (existing == null) {
            throw new BaseException(BaseCodeEnum.PURCHASE_DETAIL_NOT_FOUND);
        }
        if (existing.getPurchaseId() != null || !PurchaseDetailEnum.isNew(existing.getStatus())) {
            throw new BaseException(BaseCodeEnum.PURCHASE_DETAIL_STATUS_INVALID);
        }

        detail.setStatus(PurchaseDetailEnum.CREATED.getCode());
        detail.setPurchaseId(null);
        validate(detail);

        this.updateById(detail);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public void removeDetails(List<Long> ids) {
        List<Long> distinctIds = ids == null
                ? List.of()
                : ids.stream().filter(Objects::nonNull).distinct().toList();
        if (distinctIds.isEmpty()) {
            throw new ValidationException("ids", "请先选择要删除的采购需求单");
        }

        List<PurchaseDetailEntity> details = this.listByIds(distinctIds);
        if (details.size() != distinctIds.size()) {
            throw new BaseException(BaseCodeEnum.PURCHASE_DETAIL_NOT_FOUND);
        }

        boolean allNew = details.stream().allMatch(detail ->
                detail.getPurchaseId() == null && PurchaseDetailEnum.isNew(detail.getStatus()));
        if (!allNew) {
            throw new BaseException(BaseCodeEnum.PURCHASE_DETAIL_STATUS_INVALID);
        }

        this.removeByIds(distinctIds);
    }

    /**
     * 校验需求单的必填字段，并确认仓库与 SKU 都存在。
     *
     * @param detail 待校验的需求单，不能为 {@code null}
     * @throws common.exception.ValidationException 字段缺失或不合法、SKU 不存在时抛出
     * @throws common.exception.BaseException 仓库不存在，或商品服务不可用时抛出
     */
    private void validate(PurchaseDetailEntity detail) {
        if (detail.getSkuId() == null) {
            throw new ValidationException("skuId", "请选择采购商品");
        }
        if (detail.getSkuNum() == null || detail.getSkuNum() <= 0) {
            throw new ValidationException("skuNum", "采购数量要大于 0");
        }
        if (detail.getWareId() == null) {
            throw new ValidationException("wareId", "请选择仓库");
        }
        // skuPrice 是"这条需求的采购金额"（总额），不是单价 —— 采购单的总金额是它求和
        if (detail.getSkuPrice() == null || detail.getSkuPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException("skuPrice", "请填写采购金额");
        }
        // 仓库不存在的话，采购完成会入库到一个不存在的仓库
        if (wareInfoService.getById(detail.getWareId()) == null) {
            throw new BaseException(BaseCodeEnum.WARE_NOT_FOUND);
        }
        assertSkuExists(detail.getSkuId());
    }

    /**
     * 商品必须还在。
     *
     * <p>删商品那侧保证的是"有在途采购需求就不许删"，这里补的是反方向：商品已经删了，
     * 就不该再给它建需求单 —— 否则采购完成时 {@code addStock} 会把这个 sku 的库存行重新建出来，
     * 商品没了库存却回来了。
     *
     * @param skuId 采购商品的 SKU ID，不能为 {@code null}
     * @throws common.exception.ValidationException 商品服务返回商品不存在时抛出
     * @throws common.exception.BaseException 商品服务不可用时抛出
     */
    private void assertSkuExists(Long skuId) {
        R<Map<String, Object>> r;
        try {
            r = productFeignService.getProduct(skuId);
        } catch (Exception e) {
            // 读不到商品服务时不许建单：宁可建不了，也不要建出一条指向不存在商品的需求
            throw new BaseException("商品服务暂时不可用，无法确认商品是否存在，请稍后重试");
        }
        if (r == null || r.getCode() != 0 || r.getData() == null) {
            throw new ValidationException("skuId", "商品不存在");
        }
    }

}
