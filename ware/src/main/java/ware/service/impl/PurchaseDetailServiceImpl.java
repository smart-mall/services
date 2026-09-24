package ware.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import common.exception.BaseCodeEnum;
import common.exception.BaseException;
import common.exception.ValidationException;
import common.utils.PageUtils;
import common.utils.Query;
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


@Service("purchaseDetailService")
public class PurchaseDetailServiceImpl extends ServiceImpl<PurchaseDetailDao, PurchaseDetailEntity> implements PurchaseDetailService {
    private final ProductFeignService productFeignService;
    private final WareInfoService wareInfoService;

    public PurchaseDetailServiceImpl(ProductFeignService productFeignService, WareInfoService wareInfoService) {
        this.productFeignService = productFeignService;
        this.wareInfoService = wareInfoService;
    }

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        LambdaQueryWrapper<PurchaseDetailEntity> queryWrapper = new LambdaQueryWrapper<>();

        String key = (String) params.get("key");

        String status = (String) params.get("status");
        if (status != null && !status.isEmpty()) {
            queryWrapper.eq(PurchaseDetailEntity::getStatus, status);
        }

        String wareId = (String) params.get("wareId");
        if (wareId != null && !wareId.isEmpty()) {
            queryWrapper.eq(PurchaseDetailEntity::getWareId, wareId);
        }

        // 按采购单查它下面的明细。"完成采购"要逐条填结果，前端必须能只取一张单的明细
        String purchaseId = (String) params.get("purchaseId");
        if (purchaseId != null && !purchaseId.isEmpty()) {
            queryWrapper.eq(PurchaseDetailEntity::getPurchaseId, purchaseId);
        }

        IPage<PurchaseDetailEntity> page = this.page(
                new Query<PurchaseDetailEntity>().getPage(params),
                queryWrapper
        );

        // 添加wareName
        List<WareInfoEntity> list = wareInfoService.list();
        page.getRecords().forEach(item -> {
            item.setWareName(list.stream()
                    .filter(ware -> ware.getId().equals(item.getWareId()))
                    .findFirst()
                    .map(WareInfoEntity::getName)
                    .orElse(""));
            item.setAllowedActions(PurchaseDetailEnum.allowedActions(item.getStatus()));
        });

        // 添加skuName
        List<Long> skuIds = page.getRecords().stream()
                .map(PurchaseDetailEntity::getSkuId)
                .filter(Objects::nonNull)      // 过滤 null 值
                .distinct()                    // 去重
                .toList();

        if (!skuIds.isEmpty()) {
            R r = productFeignService.getSkuNames(skuIds);

            if (r.getCode() != 0) {
                throw new BaseException("远程服务异常" + r.getMsg());
            }

            Map<Long, String> skuNameMap = r.getData(new TypeReference<>() {
            });

            page.getRecords().forEach(item -> item.setSkuName(skuNameMap.get(item.getSkuId())));
        }


        // 关键字过滤。skuName 是远程查出来的，可能为 null，直接 contains 会 NPE
        if (key != null && !key.trim().isEmpty()) {
            page.getRecords().removeIf(item -> {
                String skuName = Objects.toString(item.getSkuName(), "");
                String skuId = Objects.toString(item.getSkuId(), "");
                return !skuName.contains(key) && !skuId.contains(key);
            });
        }

        return new PageUtils(page);
    }

    /**
     * 新增采购需求单（人工提单）。状态和归属由服务端定，不接受前端传 ——
     * 否则可以直接造一条"已完成"或"已并到某单"的需求单，把状态流转整个绕过。
     */
    @Override
    @Transactional
    public void saveDetail(PurchaseDetailEntity detail) {
        detail.setId(null);
        detail.setStatus(PurchaseDetailEnum.CREATED.getCode());
        detail.setPurchaseId(null);
        validate(detail);

        this.save(detail);
    }

    /**
     * 修改采购需求单：只在"新建"（还没并进任何采购单）时允许。
     *
     * <p>并入采购单之后采购员已经照着它在买了，这时候改数量/仓库会出现
     * "买 10 件、系统入库 100 件"。要改就先取消分配退回"新建"。</p>
     */
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
    }

}
