package ware.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import common.exception.BaseCodeEnum;
import common.exception.BaseException;
import common.exception.ValidationException;
import common.utils.PageUtils;
import common.utils.Query;
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

@Slf4j
@Service("wareInfoService")
public class WareInfoServiceImpl extends ServiceImpl<WareInfoDao, WareInfoEntity> implements WareInfoService {
    public final MemberFeignService memberFeignService;

    private final WareSkuDao wareSkuDao;
    private final PurchaseDao purchaseDao;
    private final PurchaseDetailDao purchaseDetailDao;
    private final WareOrderTaskDetailDao wareOrderTaskDetailDao;

    public WareInfoServiceImpl(MemberFeignService memberFeignService, WareSkuDao wareSkuDao,
                               PurchaseDao purchaseDao, PurchaseDetailDao purchaseDetailDao,
                               WareOrderTaskDetailDao wareOrderTaskDetailDao) {
        this.memberFeignService = memberFeignService;
        this.wareSkuDao = wareSkuDao;
        this.purchaseDao = purchaseDao;
        this.purchaseDetailDao = purchaseDetailDao;
        this.wareOrderTaskDetailDao = wareOrderTaskDetailDao;
    }

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        LambdaQueryWrapper<WareInfoEntity> queryWrapper = new LambdaQueryWrapper<>();

        String key = (String) params.get("key");
        if (key != null && !key.isEmpty()) {
            queryWrapper
                    .eq(WareInfoEntity::getId, key)
                    .or()
                    .like(WareInfoEntity::getName, key)
                    .or()
                    .like(WareInfoEntity::getAddress, key);
        }

        IPage<WareInfoEntity> page = this.page(
                new Query<WareInfoEntity>().getPage(params),
                queryWrapper
        );

        return new PageUtils(page);
    }

    /**
     * 删除仓库。三条都满足才允许：
     *
     * <ol>
     *   <li>库存全为 0，<b>含锁定库存</b> —— 还有订单锁着货的仓库不能删</li>
     *   <li>关联的采购需求和采购单都在终态（已完成 / 采购失败 / 有异常），没有在途采购</li>
     *   <li>没有"已锁定未解锁"的库存工作单明细 —— 删了仓库，订单解锁时按 detailId 找不到凭证，
     *       消息被正常 ack，{@code stock_locked} 就永远减不回去了</li>
     * </ol>
     *
     * <p>都通过之后，把该仓库的库存行、采购需求、采购单一并删掉 —— 它们都是空壳或终态记录，
     * 留着只会让 {@code ware_id} 悬空，而悬空的仓库引用没有任何意义。</p>
     */
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

            // 1、库存必须清空
            List<WareSkuEntity> stocks = wareSkuDao.selectList(new LambdaQueryWrapper<WareSkuEntity>()
                    .eq(WareSkuEntity::getWareId, wareId));
            boolean hasStock = stocks.stream().anyMatch(stock ->
                    (stock.getStock() != null && stock.getStock() != 0)
                            || (stock.getStockLocked() != null && stock.getStockLocked() != 0));
            if (hasStock) {
                throw new BaseException(BaseCodeEnum.WARE_IN_USE,
                        "仓库「" + ware.getName() + "」还有库存（或被订单锁定的库存），不能删除");
            }

            // 2、采购需求必须在终态
            List<PurchaseDetailEntity> details = purchaseDetailDao.selectList(
                    new LambdaQueryWrapper<PurchaseDetailEntity>()
                            .eq(PurchaseDetailEntity::getWareId, wareId));
            if (details.stream().anyMatch(detail -> !PurchaseDetailEnum.isFinal(detail.getStatus()))) {
                throw new BaseException(BaseCodeEnum.WARE_IN_USE,
                        "仓库「" + ware.getName() + "」还有没走完的采购需求，不能删除");
            }

            // 3、采购单必须在终态（ware_id 直接指向这个仓库的也一起算上）
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

            // 4、不能有已锁定未解锁的库存工作单明细
            Long lockedTasks = wareOrderTaskDetailDao.selectCount(new LambdaQueryWrapper<WareOrderTaskDetailEntity>()
                    .eq(WareOrderTaskDetailEntity::getWareId, wareId)
                    .eq(WareOrderTaskDetailEntity::getLockStatus, 1));
            if (lockedTasks > 0) {
                throw new BaseException(BaseCodeEnum.WARE_IN_USE,
                        "仓库「" + ware.getName() + "」还有 " + lockedTasks
                                + " 条已锁定未解锁的库存工作单，不能删除");
            }

            // 都过了：库存行、采购需求、采购单一起删
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

    @Override
    public FareVo getFare(Long addrId) {

        FareVo fareVo = new FareVo();

        //收获地址的详细信息
        R addrInfo = memberFeignService.info(addrId);
        log.info("收获地址信息：{}", JSON.toJSONString(addrInfo, SerializerFeature.PrettyFormat));

        MemberAddressVo memberAddressVo = addrInfo.getData("memberReceiveAddress",new TypeReference<MemberAddressVo>() {});

        if (memberAddressVo != null) {
            String phone = memberAddressVo.getPhone();
            if (phone == null || phone.length() < 10) {
                // 运费算法本身是 demo：取手机号倒数第 10~8 位当金额。
                // 但手机号可能为空或长度不足（历史数据、测试数据），原来直接 substring 会
                // StringIndexOutOfBoundsException —— 在 order 那边表现为一个没有 code 的 500，
                // 整个结算页打不开。这里按 0 处理并留一条日志。
                log.warn("收货地址 {} 的手机号无法用于计算运费，按 0 处理：{}", addrId, phone);
                fareVo.setFare(BigDecimal.ZERO);
            } else {
                //截取用户手机号码最后一位作为我们的运费计算
                String fare = phone.substring(phone.length() - 10, phone.length()-8);
                fareVo.setFare(new BigDecimal(fare));
            }

            fareVo.setAddress(memberAddressVo);

            return fareVo;
        }
        return null;
    }
}
