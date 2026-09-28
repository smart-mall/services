package ware.service.impl;

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
import ware.constants.PurchaseDetailEnum;
import ware.constants.PurchaseStatusEnum;
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
import ware.feign.ThirdPartyFeignService;
import ware.service.WareInfoService;
import ware.vo.FareItemResultVo;
import ware.vo.FareQueryItemVo;
import ware.vo.FareQueryVo;
import ware.vo.FareVo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
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

    /** 运费档位的距离上限，单位公里，与 {@link #TIER_FARES} 一一对应。 */
    private static final BigDecimal[] TIER_MAX_KM = {
            new BigDecimal("50"), new BigDecimal("200"), new BigDecimal("500"),
            new BigDecimal("1000"), new BigDecimal("2000")};

    /** 各档的基准运费，单位元；比 {@link #TIER_MAX_KM} 多一项，末项是超出最后一档的价。 */
    private static final BigDecimal[] TIER_FARES = {
            new BigDecimal("8.00"), new BigDecimal("10.00"), new BigDecimal("12.00"),
            new BigDecimal("15.00"), new BigDecimal("18.00"), new BigDecimal("22.00")};

    /** 续件加价比例：每多一件在第一件运费的基础上加两成，不是每件都收一份全额。 */
    private static final BigDecimal EXTRA_ITEM_RATE = new BigDecimal("0.2");

    private final ThirdPartyFeignService thirdPartyFeignService;
    private final WareSkuDao wareSkuDao;
    private final PurchaseDao purchaseDao;
    private final PurchaseDetailDao purchaseDetailDao;
    private final WareOrderTaskDetailDao wareOrderTaskDetailDao;

    /**
     * 由容器注入地理客户端与四张关联表的 Mapper 构造。
     *
     * @param thirdPartyFeignService 第三方服务客户端，按仓库与收货地的区划编码取直线距离
     * @param wareSkuDao 库存 Mapper，删除仓库前查库存占用并清理该仓的库存行，计费时取候选仓
     * @param purchaseDao 采购单 Mapper，删除仓库前查采购单状态并清理该仓的采购单
     * @param purchaseDetailDao 采购需求单 Mapper，删除仓库前查需求状态并清理该仓的需求
     * @param wareOrderTaskDetailDao 库存工作单明细 Mapper，删除仓库前查有没有已锁定未解锁的明细
     */
    public WareInfoServiceImpl(ThirdPartyFeignService thirdPartyFeignService, WareSkuDao wareSkuDao,
                               PurchaseDao purchaseDao, PurchaseDetailDao purchaseDetailDao,
                               WareOrderTaskDetailDao wareOrderTaskDetailDao) {
        this.thirdPartyFeignService = thirdPartyFeignService;
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
    public FareVo getFare(FareQueryVo query) {
        List<FareQueryItemVo> items = query.getItems();
        if (items == null || items.isEmpty()) {
            // 空清单直接给 0：空集合拼进 IN 子句会得到 IN ()，那是 SQL 语法错误不是空结果
            FareVo empty = new FareVo();
            empty.setTotalFare(BigDecimal.ZERO);
            empty.setItems(List.of());
            return empty;
        }

        // 1. 取候选仓：只看有没有库存记录，不看实时可售量，保证同一组入参算出同一结果
        List<Long> skuIds = items.stream().map(FareQueryItemVo::getSkuId).distinct().toList();
        List<WareSkuEntity> stockRows = wareSkuDao.selectList(
                new LambdaQueryWrapper<WareSkuEntity>().in(WareSkuEntity::getSkuId, skuIds));
        Map<Long, List<Long>> wareIdsBySku = stockRows.stream()
                .collect(Collectors.groupingBy(WareSkuEntity::getSkuId,
                        Collectors.mapping(WareSkuEntity::getWareId, Collectors.toList())));

        // 2. 一次问全所有候选仓到收货地的距离：按仓库逐个往返，商品一多就是 N 次远程调用
        Map<Long, String> nodesByWare = listWareNodes(
                stockRows.stream().map(WareSkuEntity::getWareId).distinct().toList());
        Map<String, BigDecimal> distances = listDistances(query.getDestNode(), nodesByWare.values());

        // 3. 逐商品选最近的计费仓并定价；整单运费在这里汇总，调用方不再自己加
        List<FareItemResultVo> results = new ArrayList<>();
        BigDecimal totalFare = BigDecimal.ZERO;
        for (FareQueryItemVo item : items) {
            FareItemResultVo result = calcItemFare(item, wareIdsBySku, nodesByWare, distances);
            results.add(result);
            totalFare = totalFare.add(result.getFare());
        }

        FareVo fareVo = new FareVo();
        fareVo.setTotalFare(totalFare);
        fareVo.setItems(results);
        return fareVo;
    }

    /**
     * 查询指定仓库的区划编码。
     *
     * @param wareIds 仓库 ID 列表，不能为 {@code null}
     * @return 仓库 ID 到区划编码的映射；仓库不存在或没配编码的不会出现在结果里
     */
    private Map<Long, String> listWareNodes(List<Long> wareIds) {
        if (wareIds.isEmpty()) {
            return Map.of();
        }
        return listByIds(wareIds).stream()
                .filter(ware -> ware.getAreacode() != null && !ware.getAreacode().isBlank())
                .collect(Collectors.toMap(WareInfoEntity::getId, WareInfoEntity::getAreacode));
    }

    /**
     * 批量查询收货地到各仓库区划的直线距离。
     *
     * @param destNode 收货地区划编码，不能为 {@code null}
     * @param wareNodes 仓库区划编码集合，不能为 {@code null}
     * @return 区划编码到直线距离（单位公里）的映射；取不到坐标的编码不在结果里
     * @throws BaseException 地理服务不可用或返回非 0 码时抛 {@code WARE_FARE_DISTANCE_FAILED}
     */
    private Map<String, BigDecimal> listDistances(String destNode, Collection<String> wareNodes) {
        List<String> distinctNodes = wareNodes.stream().distinct().toList();
        if (distinctNodes.isEmpty()) {
            return Map.of();
        }
        R<Map<String, BigDecimal>> response = thirdPartyFeignService.getDistance(destNode, distinctNodes);
        if (response.getCode() != 0 || response.getData() == null) {
            // 原样带上地理服务的 msg：区分"编码不存在"和"服务不可用"，两者排查方向完全不同
            throw new BaseException(BaseCodeEnum.WARE_FARE_DISTANCE_FAILED, response.getMsg());
        }
        return response.getData();
    }

    /**
     * 计算单个商品的运费，候选仓里选距离最近的一个作为计费仓。
     *
     * @param item 商品与数量，不能为 {@code null}
     * @param wareIdsBySku 商品到候选仓 ID 的映射，不能为 {@code null}
     * @param nodesByWare 仓库到区划编码的映射，不能为 {@code null}
     * @param distances 区划编码到距离的映射，不能为 {@code null}
     * @return 该商品的运费明细
     * @throws BaseException 商品没有任何候选仓时抛 {@code WARE_FARE_NO_WAREHOUSE}；
     *         候选仓都取不到距离时抛 {@code WARE_FARE_WAREHOUSE_NO_AREA}
     */
    private FareItemResultVo calcItemFare(FareQueryItemVo item, Map<Long, List<Long>> wareIdsBySku,
                                          Map<Long, String> nodesByWare, Map<String, BigDecimal> distances) {
        List<Long> candidates = wareIdsBySku.get(item.getSkuId());
        if (candidates == null || candidates.isEmpty()) {
            throw new BaseException(BaseCodeEnum.WARE_FARE_NO_WAREHOUSE, "商品 " + item.getSkuId() + " 没有库存记录");
        }

        Long nearestWareId = null;
        BigDecimal nearestDistance = null;
        for (Long wareId : candidates) {
            String node = nodesByWare.get(wareId);
            BigDecimal distance = node == null ? null : distances.get(node);
            // 取不到距离的仓排除在外：某个仓没配区划编码不该让整单算不出运费
            if (distance == null) {
                continue;
            }
            if (nearestDistance == null || distance.compareTo(nearestDistance) < 0) {
                nearestDistance = distance;
                nearestWareId = wareId;
            }
        }
        if (nearestWareId == null) {
            throw new BaseException(BaseCodeEnum.WARE_FARE_WAREHOUSE_NO_AREA,
                    "商品 " + item.getSkuId() + " 的候选仓都取不到距离");
        }

        FareItemResultVo result = new FareItemResultVo();
        result.setSkuId(item.getSkuId());
        result.setFare(calcFare(nearestDistance, item.getNum()));
        result.setWareId(nearestWareId);
        result.setDistanceKm(nearestDistance);
        return result;
    }

    /**
     * 按计费仓的距离与购买数量算单个商品的运费。
     *
     * <p>续件加价在第一件运费的基础上按比例加，不是每件都收一份全额：基准 7 元买 4 件是
     * {@code 7 × (1 + 0.2 × 3) = 11.2} 元。只在最后舍入一次，中途舍入会让确认页与提交时算出两个数。
     *
     * @param distanceKm 收货地到计费仓的直线距离，单位公里，不能为 {@code null}
     * @param num 购买数量，必须大于 0
     * @return 该商品的运费，单位元，保留 2 位小数
     */
    private BigDecimal calcFare(BigDecimal distanceKm, Integer num) {
        BigDecimal baseFare = resolveBaseFare(distanceKm);
        BigDecimal extraRate = EXTRA_ITEM_RATE.multiply(BigDecimal.valueOf(num - 1L));
        return baseFare.multiply(BigDecimal.ONE.add(extraRate)).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 按距离取基准运费。
     *
     * <p>用分档而不是连续函数：档位能写死预期值、便于验证，也贴近实际快递按区域报价的做法。
     *
     * @param distanceKm 收货地到计费仓的直线距离，单位公里，不能为 {@code null}
     * @return 基准运费，单位元
     */
    private BigDecimal resolveBaseFare(BigDecimal distanceKm) {
        for (int i = 0; i < TIER_MAX_KM.length; i++) {
            if (distanceKm.compareTo(TIER_MAX_KM[i]) <= 0) {
                return TIER_FARES[i];
            }
        }
        return TIER_FARES[TIER_FARES.length - 1];
    }
}
