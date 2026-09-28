package ware.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import common.exception.NoStockException;
import common.exception.ValidationException;
import common.mq.MqConstant;
import common.mq.MqPublisher;
import common.to.OrderTo;
import common.to.SkuDeleteBlockerTo;
import common.to.mq.StockDetailTo;
import common.to.mq.StockLockedTo;
import common.vo.PageVO;
import common.utils.R;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ware.costant.PurchaseDetailEnum;
import ware.dao.PurchaseDetailDao;
import ware.dao.WareSkuDao;
import ware.entity.PurchaseDetailEntity;
import ware.entity.WareInfoEntity;
import ware.entity.WareOrderTaskDetailEntity;
import ware.entity.WareOrderTaskEntity;
import ware.entity.WareSkuEntity;
import ware.feign.OrderFeignService;
import ware.feign.ProductFeignService;
import ware.service.WareInfoService;
import ware.service.WareOrderTaskDetailService;
import ware.service.WareOrderTaskService;
import ware.service.WareSkuService;
import ware.vo.OrderItemVo;
import ware.vo.OrderVo;
import ware.vo.SkuHasStockVo;
import ware.vo.WareSkuLockVo;

import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;


import ware.vo.WareSkuPageQuery;
/**
 * 商品库存服务的默认实现，基于 MyBatis-Plus 的 {@code ServiceImpl} 读写 {@code wms_ware_sku}。
 *
 * <p>库存增量、锁定与解锁都走 {@link WareSkuDao} 的自定义 SQL，在数据库侧完成加减；
 * 锁定与解锁会同时写库存工作单及其明细，并通过 MQ 投递锁定事件。
 */
@Slf4j
@Service("wareSkuService")
public class WareSkuServiceImpl extends ServiceImpl<WareSkuDao, WareSkuEntity> implements WareSkuService {

    private final WareSkuDao wareSkuDao;
    private final ProductFeignService productFeignService;
    private final MqPublisher mqPublisher;
    private final WareOrderTaskService wareOrderTaskService;
    private final WareOrderTaskDetailService wareOrderTaskDetailService;
    private final OrderFeignService orderFeignService;
    private final WareInfoService wareInfoService;
    private final PurchaseDetailDao purchaseDetailDao;

    /**
     * 由容器注入库存 Mapper、远程客户端与两个工作单服务构造。
     *
     * @param wareSkuDao 库存 Mapper，库存增量、锁定与解锁都走它的自定义 SQL
     * @param productFeignService 商品服务客户端，新建库存行时取 SKU 名称
     * @param mqPublisher 消息发布器，锁定成功后投递延迟解锁消息
     * @param wareOrderTaskService 库存工作单服务，锁定前建工作单
     * @param wareOrderTaskDetailService 库存工作单明细服务，记录并回写每条锁定
     * @param orderFeignService 订单服务客户端，解锁前查订单状态
     * @param wareInfoService 仓库服务，库存列表与阻塞清单补仓库名
     * @param purchaseDetailDao 采购需求单 Mapper，判断 SKU 是否还有在途采购
     */
    public WareSkuServiceImpl(WareSkuDao wareSkuDao, ProductFeignService productFeignService, MqPublisher mqPublisher, WareOrderTaskService wareOrderTaskService, WareOrderTaskDetailService wareOrderTaskDetailService, OrderFeignService orderFeignService, WareInfoService wareInfoService, PurchaseDetailDao purchaseDetailDao) {
        this.wareSkuDao = wareSkuDao;
        this.productFeignService = productFeignService;
        this.mqPublisher = mqPublisher;
        this.wareOrderTaskService = wareOrderTaskService;
        this.wareOrderTaskDetailService = wareOrderTaskDetailService;
        this.orderFeignService = orderFeignService;
        this.wareInfoService = wareInfoService;
        this.purchaseDetailDao = purchaseDetailDao;
    }

    /** {@inheritDoc} */
    @Override
    public PageVO<WareSkuEntity> queryPage(WareSkuPageQuery query) {
        List<WareInfoEntity> wareInfoEntities = wareInfoService.list();

        LambdaQueryWrapper<WareSkuEntity> queryWrapper = new LambdaQueryWrapper<>();

        String key = query.getSkuId();
        if (key != null && !key.isEmpty()) {
            queryWrapper.eq(WareSkuEntity::getSkuId, key);
        }

        String wareId = query.getWareId();
        if (wareId != null && !wareId.isEmpty()) {
            queryWrapper.eq(WareSkuEntity::getWareId, wareId);
        }

        IPage<WareSkuEntity> page = this.page(
                query.toPage(),
                queryWrapper
        );

        // 仓库被删过的话这里找不到（历史脏数据），用 orElse 兜住，别让只读的库存页 500
        page.getRecords().forEach(wareSkuEntity -> wareSkuEntity.setWareName(wareInfoEntities.stream()
                .filter(wareInfoEntity -> wareInfoEntity.getId().equals(wareSkuEntity.getWareId()))
                .findFirst()
                .map(WareInfoEntity::getName)
                .orElse(null)));

        return new PageVO<>(page.getTotal(), page.getRecords());
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public void addStock(Long skuId, Long wareId, Integer skuNum) {
        if (skuId == null || wareId == null || skuNum == null || skuNum <= 0) {
            throw new ValidationException("skuNum", "入库的 sku、仓库和数量都不能为空，且数量要大于 0");
        }

        LambdaQueryWrapper<WareSkuEntity> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WareSkuEntity::getSkuId, skuId)
                .eq(WareSkuEntity::getWareId, wareId);

        if (baseMapper.selectCount(queryWrapper) == 0) {
            // 这个仓库还没有这个 sku 的库存行，先建一行（数量为 0），增量交给下面的语句
            WareSkuEntity wareSkuEntity = new WareSkuEntity();
            wareSkuEntity.setSkuId(skuId);
            wareSkuEntity.setWareId(wareId);
            wareSkuEntity.setStock(0);
            wareSkuEntity.setStockLocked(0);
            try {
                R<Map<String, Object>> info = productFeignService.getProduct(skuId);

                Map<String, Object> skuInfo = info.getData();
                if (info.getCode() == 0 && skuInfo != null) {
                    wareSkuEntity.setSkuName((String) skuInfo.get("skuName"));
                }
            } catch (Exception ignored) {
                // 拿不到名字不影响入库，只是列表里少个显示名
            }
            baseMapper.insert(wareSkuEntity);
        }

        // 增量必须走 SQL 的 stock = stock + #{skuNum}：先查再算会丢并发更新，
        // 两个请求同时入库时后写的会把先写的增量覆盖掉
        wareSkuDao.addStock(skuId, wareId, skuNum);
    }

    /** {@inheritDoc} */
    @Override
    public List<SkuHasStockVo> getSkusHasStock(List<Long> skuIds) {
        return skuIds.stream().map(skuId -> {
            SkuHasStockVo skuHasStockVo = new SkuHasStockVo();
            Long count = baseMapper.getSkuStock(skuId);
            skuHasStockVo.setSkuId(skuId);
            skuHasStockVo.setHasStock(count != null && count > 0);
            return skuHasStockVo;
        }).toList();
    }

    /** {@inheritDoc} */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean orderLockStock(WareSkuLockVo vo) {
        // 1. 先建库存工作单：这一单锁定的所有明细都挂在它下面
        WareOrderTaskEntity wareOrderTaskEntity = new WareOrderTaskEntity();
        wareOrderTaskEntity.setOrderSn(vo.getOrderSn());
        wareOrderTaskEntity.setCreateTime(new Date());
        wareOrderTaskService.save(wareOrderTaskEntity);


        // 2. 查出每个 SKU 有货的仓库列表，后面逐个尝试锁定
        List<OrderItemVo> locks = vo.getLocks();

        List<SkuWareHasStock> collect = locks.stream().map((item) -> {
            SkuWareHasStock stock = new SkuWareHasStock();
            Long skuId = item.getSkuId();
            stock.setSkuId(skuId);
            stock.setNum(item.getCount());
            List<Long> wareIdList = wareSkuDao.listWareIdHasSkuStock(skuId);
            stock.setWareId(wareIdList);

            return stock;
        }).toList();

        // 3. 逐个 SKU 锁定：每个 SKU 只锁一个仓库，锁不上就整单失败
        for (SkuWareHasStock hasStock : collect) {
            boolean skuStocked = false;
            Long skuId = hasStock.getSkuId();
            List<Long> wareIds = hasStock.getWareId();

            if (org.springframework.util.StringUtils.isEmpty(wareIds)) {
                throw new NoStockException(skuId);
            }

            for (Long wareId : wareIds) {
                // lockSkuStock 带 stock - stock_locked > 0 条件，返回 1 表示这个仓库锁上了
                Long count = wareSkuDao.lockSkuStock(skuId,wareId,hasStock.getNum());
                if (count == 1) {
                    skuStocked = true;
                    WareOrderTaskDetailEntity taskDetailEntity = WareOrderTaskDetailEntity.builder()
                            .skuId(skuId)
                            .skuName("")
                            .skuNum(hasStock.getNum())
                            .taskId(wareOrderTaskEntity.getId())
                            .wareId(wareId)
                            .lockStatus(1)
                            .build();
                    wareOrderTaskDetailService.save(taskDetailEntity);

                    StockLockedTo lockedTo = new StockLockedTo();
                    lockedTo.setId(wareOrderTaskEntity.getId());
                    StockDetailTo detailTo = new StockDetailTo();
                    BeanUtils.copyProperties(taskDetailEntity,detailTo);
                    lockedTo.setDetailTo(detailTo);
                    // 消息先于事务提交发出：若本事务最终回滚，解锁时按明细 ID 查不到记录，
                    // 那条消息就成了空操作，不会误放别单的库存
                    mqPublisher.publish(MqConstant.Exchanges.STOCK, MqConstant.RoutingKeys.STOCK_LOCKED, lockedTo);
                    break;
                } else {
                    //当前仓库锁失败，重试下一个仓库
                }
            }

            if (!skuStocked) {
                throw new NoStockException(skuId);
            }
        }

        // 4. 走到这里说明所有 SKU 都锁上了，失败的情况已在上面抛异常
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public void unlockStock(StockLockedTo to) {
        StockDetailTo detail = to.getDetailTo();
        Long detailId = detail.getId();

        WareOrderTaskDetailEntity taskDetailInfo = wareOrderTaskDetailService.getById(detailId);
        if (taskDetailInfo != null) {
            Long id = to.getId();
            WareOrderTaskEntity orderTaskInfo = wareOrderTaskService.getById(id);
            String orderSn = orderTaskInfo.getOrderSn();
            R<OrderVo> orderData = orderFeignService.getOrderStatus(orderSn);
            if (orderData.getCode() == 0) {
                OrderVo orderInfo = orderData.getData();

                // 4 是"已关闭"：订单不存在或已关闭才能放掉这批货，已支付的要留着发货
                if (orderInfo == null || orderInfo.getStatus() == 4) {
                    // 锁定状态为 1 才需要解锁；重复投递时第二次进来已是 2，不会多减一次
                    if (taskDetailInfo.getLockStatus() == 1) {
                        unLockStock(detail.getSkuId(),detail.getWareId(),detail.getSkuNum(),detailId);
                    }
                }
            } else {
                // 抛异常让监听器把消息放回队列重投：订单状态查不到时不能贸然解锁
                throw new RuntimeException("远程调用服务失败");
            }
        } else {
            // 明细不存在：锁定没成功或已经解过锁，无需处理
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>只取锁定状态为 1 的明细，已经解过锁的不再处理。
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void unlockStock(OrderTo orderTo) {

        String orderSn = orderTo.getOrderSn();
        WareOrderTaskEntity orderTaskEntity = wareOrderTaskService.getOrderTaskByOrderSn(orderSn);

        // 只取锁定状态为 1 的明细：已经解过锁的再处理一次会重复扣减 stock_locked
        Long id = orderTaskEntity.getId();
        List<WareOrderTaskDetailEntity> list = wareOrderTaskDetailService.list(new LambdaQueryWrapper<WareOrderTaskDetailEntity>()
                .eq(WareOrderTaskDetailEntity::getTaskId, id).eq(WareOrderTaskDetailEntity::getLockStatus, 1));

        for (WareOrderTaskDetailEntity taskDetailEntity : list) {
            unLockStock(taskDetailEntity.getSkuId(),
                    taskDetailEntity.getWareId(),
                    taskDetailEntity.getSkuNum(),
                    taskDetailEntity.getId());
        }

    }

    /**
     * 释放一条库存工作单明细占用的库存，并把该明细的锁定状态置为"已解锁"。
     *
     * <p>本方法自身不校验幂等，重复调用会重复扣减 {@code stock_locked}；
     * 幂等由调用方按明细的锁定状态保证。
     *
     * @param skuId SKU ID，不能为 {@code null}
     * @param wareId 仓库 ID，不能为 {@code null}
     * @param num 要释放的锁定数量，必须大于 0
     * @param taskDetailId 库存工作单明细 ID，不能为 {@code null}
     */
    public void unLockStock(Long skuId,Long wareId,Integer num,Long taskDetailId) {

        wareSkuDao.unLockStock(skuId,wareId,num);

        // 库存减回去之后把明细置为已解锁（2），下次再消费就不会重复扣减
        WareOrderTaskDetailEntity taskDetailEntity = new WareOrderTaskDetailEntity();
        taskDetailEntity.setId(taskDetailId);
        taskDetailEntity.setLockStatus(2);
        wareOrderTaskDetailService.updateById(taskDetailEntity);

    }


    /** {@inheritDoc} */
    @Override
    public List<SkuDeleteBlockerTo> canDelete(List<Long> skuIds) {
        List<Long> ids = skuIds == null ? List.of()
                : skuIds.stream().filter(Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) {
            return List.of();
        }

        // 有量才算占用：stock 为 0 的行表示这个仓库当前没货，删商品时顺手清掉即可
        Map<Long, List<WareSkuEntity>> stockBySku = wareSkuDao.selectList(
                        new LambdaQueryWrapper<WareSkuEntity>()
                                .in(WareSkuEntity::getSkuId, ids)
                                .and(w -> w.gt(WareSkuEntity::getStock, 0)
                                        .or().gt(WareSkuEntity::getStockLocked, 0)))
                .stream()
                .collect(Collectors.groupingBy(WareSkuEntity::getSkuId));

        // 只算没走完的：已完成和采购失败都不拦 —— 前者货已入库、库存那条会拦，
        // 后者没产生库存。status 可能是 null（生成器建的行没写），按没走完算
        Map<Long, Long> purchaseCountBySku = purchaseDetailDao.selectList(
                        new LambdaQueryWrapper<PurchaseDetailEntity>()
                                .in(PurchaseDetailEntity::getSkuId, ids)
                                .and(w -> w.isNull(PurchaseDetailEntity::getStatus)
                                        .or().notIn(PurchaseDetailEntity::getStatus,
                                                PurchaseDetailEnum.FINISH.getCode(),
                                                PurchaseDetailEnum.HASERROR.getCode())))
                .stream()
                .collect(Collectors.groupingBy(PurchaseDetailEntity::getSkuId, Collectors.counting()));

        Set<Long> blockedSkuIds = new LinkedHashSet<>(stockBySku.keySet());
        blockedSkuIds.addAll(purchaseCountBySku.keySet());
        if (blockedSkuIds.isEmpty()) {
            return List.of();
        }

        Map<Long, String> wareNames = wareNamesOf(stockBySku.values().stream()
                .flatMap(List::stream)
                .map(WareSkuEntity::getWareId)
                .filter(Objects::nonNull)
                .distinct()
                .toList());

        return blockedSkuIds.stream().map(skuId -> {
            SkuDeleteBlockerTo vo = new SkuDeleteBlockerTo();
            vo.setSkuId(skuId);
            vo.setStockBlockers(stockBySku.getOrDefault(skuId, List.of()).stream().map(row -> {
                SkuDeleteBlockerTo.StockBlocker blocker = new SkuDeleteBlockerTo.StockBlocker();
                blocker.setWareId(row.getWareId());
                blocker.setWareName(wareNames.get(row.getWareId()));
                blocker.setStock(row.getStock());
                blocker.setStockLocked(row.getStockLocked());
                return blocker;
            }).toList());
            vo.setPurchaseBlockerCount(purchaseCountBySku.getOrDefault(skuId, 0L).intValue());
            return vo;
        }).toList();
    }

    /**
     * 按仓库 ID 批量查仓库名。
     *
     * <p>手工装 Map 而不用 {@code Collectors.toMap}：后者遇到值为 {@code null} 的仓库名会抛 {@code NullPointerException}。
     *
     * @param wareIds 仓库 ID 列表，不能为 {@code null}；空列表返回空 Map
     * @return 仓库 ID 到仓库名的映射；查不到的仓库不会出现在结果里
     */
    private Map<Long, String> wareNamesOf(List<Long> wareIds) {
        Map<Long, String> names = new HashMap<>();
        if (wareIds.isEmpty()) {
            return names;
        }
        for (WareInfoEntity ware : wareInfoService.listByIds(wareIds)) {
            names.put(ware.getId(), ware.getName());
        }
        return names;
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public int deleteZeroStock(List<Long> skuIds) {
        List<Long> ids = skuIds == null ? List.of()
                : skuIds.stream().filter(Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) {
            return 0;
        }

        // 有量的一律不动：宁可留一行脏数据，也不能删掉真实的货。
        // 正常流程走不到这里 —— 判断会把有量的 sku 先拦掉；真查到了说明判断和删除之间出了岔子
        Long kept = wareSkuDao.selectCount(new LambdaQueryWrapper<WareSkuEntity>()
                .in(WareSkuEntity::getSkuId, ids)
                .and(w -> w.gt(WareSkuEntity::getStock, 0).or().gt(WareSkuEntity::getStockLocked, 0)));
        if (kept != null && kept > 0) {
            log.warn("清库存行时跳过了 {} 行有库存的记录，skuIds={}", kept, ids);
        }

        // 删除条件正好是上面拦截条件的取反，两边都不漏。stock 和 stock_locked 都可能为 null
        return wareSkuDao.delete(new LambdaQueryWrapper<WareSkuEntity>()
                .in(WareSkuEntity::getSkuId, ids)
                .nested(w -> w.isNull(WareSkuEntity::getStock).or().le(WareSkuEntity::getStock, 0))
                .nested(w -> w.isNull(WareSkuEntity::getStockLocked).or().le(WareSkuEntity::getStockLocked, 0)));
    }

    /** 锁定库存的中间态：一个 SKU 的待锁数量与它有货的仓库列表。 */
    @Data
    static
    class SkuWareHasStock {
        /** SKU ID。 */
        private Long skuId;
        /** 待锁定的数量。 */
        private Integer num;
        /** 有该 SKU 库存的仓库 ID 列表。 */
        private List<Long> wareId;
    }

}