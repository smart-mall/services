package ware.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import common.exception.BaseCodeEnum;
import common.exception.BaseException;
import common.exception.ValidationException;
import common.vo.PageVO;
import common.utils.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ware.costant.PurchaseDetailEnum;
import ware.costant.PurchaseStatusEnum;
import ware.dao.PurchaseDao;
import ware.dao.PurchaseDetailDao;
import ware.dao.WareInfoDao;
import ware.dao.WareOrderTaskDetailDao;
import ware.dao.WareSkuDao;
import ware.entity.PurchaseDetailEntity;
import ware.entity.PurchaseEntity;
import ware.entity.WareInfoEntity;
import ware.entity.WareOrderTaskDetailEntity;
import ware.entity.WareSkuEntity;
import ware.feign.MemberFeignService;
import ware.service.WareInfoService;
import ware.vo.FareVo;
import ware.vo.MemberAddressVo;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import common.query.KeyPageQuery;
/**
 * 仓库信息服务的默认实现，基于 MyBatis-Plus 的 {@code ServiceImpl} 读写 {@code wms_ware_info}。
 *
 * <p>删除仓库要跨库存、采购需求、采购单与库存工作单明细四张表做占用检查，
 * 全部通过后在事务内连同该仓的附属记录一起删除。
 */
@Slf4j
@Service("wareInfoService")
public class WareInfoServiceImpl extends ServiceImpl<WareInfoDao, WareInfoEntity> implements WareInfoService {
    public final MemberFeignService memberFeignService;

    private final WareSkuDao wareSkuDao;
    private final PurchaseDao purchaseDao;
    private final PurchaseDetailDao purchaseDetailDao;
    private final WareOrderTaskDetailDao wareOrderTaskDetailDao;

    /**
     * 由容器注入会员客户端与四张关联表的 Mapper 构造。
     *
     * @param memberFeignService 会员服务客户端，按收货地址取运费计算所需的手机号
     * @param wareSkuDao 库存 Mapper，删除仓库前查库存占用并清理该仓的库存行
     * @param purchaseDao 采购单 Mapper，删除仓库前查采购单状态并清理该仓的采购单
     * @param purchaseDetailDao 采购需求单 Mapper，删除仓库前查需求状态并清理该仓的需求
     * @param wareOrderTaskDetailDao 库存工作单明细 Mapper，删除仓库前查有没有已锁定未解锁的明细
     */
    public WareInfoServiceImpl(MemberFeignService memberFeignService, WareSkuDao wareSkuDao,
                               PurchaseDao purchaseDao, PurchaseDetailDao purchaseDetailDao,
                               WareOrderTaskDetailDao wareOrderTaskDetailDao) {
        this.memberFeignService = memberFeignService;
        this.wareSkuDao = wareSkuDao;
        this.purchaseDao = purchaseDao;
        this.purchaseDetailDao = purchaseDetailDao;
        this.wareOrderTaskDetailDao = wareOrderTaskDetailDao;
    }

    /** {@inheritDoc} */
    @Override
    public PageVO<WareInfoEntity> queryPage(KeyPageQuery query) {
        LambdaQueryWrapper<WareInfoEntity> queryWrapper = new LambdaQueryWrapper<>();

        String key = query.getKey();
        if (key != null && !key.isEmpty()) {
            queryWrapper
                    .eq(WareInfoEntity::getId, key)
                    .or()
                    .like(WareInfoEntity::getName, key)
                    .or()
                    .like(WareInfoEntity::getAddress, key);
        }

        IPage<WareInfoEntity> page = this.page(
                query.toPage(),
                queryWrapper
        );

        return new PageVO<>(page.getTotal(), page.getRecords());
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public void deleteByIds(List<Long> ids) {
        List<Long> distinctIds = ids == null
                ? List.of()
                : ids.stream().filter(Objects::nonNull).distinct().toList();
        if (distinctIds.isEmpty()) {
            throw new ValidationException("ids", "请先选择要删除的仓库");
        }

        List<WareInfoEntity> wares = this.listByIds(distinctIds);
        if (wares.size() != distinctIds.size()) {
            throw new BaseException(BaseCodeEnum.WARE_NOT_FOUND);
        }

        for (WareInfoEntity ware : wares) {
            Long wareId = ware.getId();

            // 1. 库存必须清空（含锁定库存）
            List<WareSkuEntity> stocks = wareSkuDao.selectList(new LambdaQueryWrapper<WareSkuEntity>()
                    .eq(WareSkuEntity::getWareId, wareId));
            boolean hasStock = stocks.stream().anyMatch(stock ->
                    (stock.getStock() != null && stock.getStock() != 0)
                            || (stock.getStockLocked() != null && stock.getStockLocked() != 0));
            if (hasStock) {
                throw new BaseException(BaseCodeEnum.WARE_IN_USE,
                        "仓库「" + ware.getName() + "」还有库存（或被订单锁定的库存），不能删除");
            }

            // 2. 采购需求必须在终态
            List<PurchaseDetailEntity> details = purchaseDetailDao.selectList(
                    new LambdaQueryWrapper<PurchaseDetailEntity>()
                            .eq(PurchaseDetailEntity::getWareId, wareId));
            if (details.stream().anyMatch(detail -> !PurchaseDetailEnum.isFinal(detail.getStatus()))) {
                throw new BaseException(BaseCodeEnum.WARE_IN_USE,
                        "仓库「" + ware.getName() + "」还有没走完的采购需求，不能删除");
            }

            // 3. 采购单必须在终态（ware_id 直接指向这个仓库的也一起算上）
            List<Long> purchaseIds = details.stream()
                    .map(PurchaseDetailEntity::getPurchaseId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toCollection(ArrayList::new));
            purchaseDao.selectList(new LambdaQueryWrapper<PurchaseEntity>()
                            .eq(PurchaseEntity::getWareId, wareId))
                    .forEach(purchase -> {
                        if (!purchaseIds.contains(purchase.getId())) {
                            purchaseIds.add(purchase.getId());
                        }
                    });
            if (!purchaseIds.isEmpty()) {
                List<PurchaseEntity> purchases = purchaseDao.selectByIds(purchaseIds);
                if (purchases.stream().anyMatch(purchase -> !PurchaseStatusEnum.isFinal(purchase.getStatus()))) {
                    throw new BaseException(BaseCodeEnum.WARE_IN_USE,
                            "仓库「" + ware.getName() + "」还有没走完的采购单，不能删除");
                }
            }

            // 4. 不能有已锁定未解锁的库存工作单明细
            Long lockedTasks = wareOrderTaskDetailDao.selectCount(new LambdaQueryWrapper<WareOrderTaskDetailEntity>()
                    .eq(WareOrderTaskDetailEntity::getWareId, wareId)
                    .eq(WareOrderTaskDetailEntity::getLockStatus, 1));
            if (lockedTasks > 0) {
                throw new BaseException(BaseCodeEnum.WARE_IN_USE,
                        "仓库「" + ware.getName() + "」还有 " + lockedTasks
                                + " 条已锁定未解锁的库存工作单，不能删除");
            }

            // 5. 都过了：库存行、采购需求、采购单一起删
            wareSkuDao.delete(new LambdaQueryWrapper<WareSkuEntity>()
                    .eq(WareSkuEntity::getWareId, wareId));
            purchaseDetailDao.delete(new LambdaQueryWrapper<PurchaseDetailEntity>()
                    .eq(PurchaseDetailEntity::getWareId, wareId));
            if (!purchaseIds.isEmpty()) {
                purchaseDao.deleteByIds(purchaseIds);
            }

            log.info("删除仓库「{}」并清掉它的 {} 条库存、{} 条采购需求、{} 张采购单",
                    ware.getName(), stocks.size(), details.size(), purchaseIds.size());
        }

        this.removeByIds(distinctIds);
    }

    /** {@inheritDoc} */
    @Override
    public FareVo getFare(Long addrId) {

        FareVo fareVo = new FareVo();

        R<MemberAddressVo> addrInfo = memberFeignService.info(addrId);
        log.info("收获地址信息：{}", JSON.toJSONString(addrInfo, SerializerFeature.PrettyFormat));

        MemberAddressVo memberAddressVo = addrInfo.getData();

        if (memberAddressVo != null) {
            String phone = memberAddressVo.getPhone();
            if (phone == null || phone.length() < 10) {
                // 手机号为空或不足 10 位时下面的 substring 会越界；越界异常在 order 侧表现为
                // 一个没有 code 的 500，整个结算页打不开，所以这里按 0 处理并留一条日志
                log.warn("收货地址 {} 的手机号无法用于计算运费，按 0 处理：{}", addrId, phone);
                fareVo.setFare(BigDecimal.ZERO);
            } else {
                // 运费取手机号倒数第 10、9 位组成的两位数字
                String fare = phone.substring(phone.length() - 10, phone.length()-8);
                fareVo.setFare(new BigDecimal(fare));
            }

            fareVo.setAddress(memberAddressVo);

            return fareVo;
        }
        return null;
    }
}
