package ware.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
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
import common.utils.Query;
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

    @Override
    public PageVO<WareSkuEntity> queryPage(Map<String, Object> params) {
        List<WareInfoEntity> wareInfoEntities = wareInfoService.list();

        LambdaQueryWrapper<WareSkuEntity> queryWrapper = new LambdaQueryWrapper<>();

        String key = (String) params.get("skuId");
        if (key != null && !key.isEmpty()) {
            queryWrapper.eq(WareSkuEntity::getSkuId, key);
        }

        String wareId = (String) params.get("wareId");
        if (wareId != null && !wareId.isEmpty()) {
            queryWrapper.eq(WareSkuEntity::getWareId, wareId);
        }

        IPage<WareSkuEntity> page = this.page(
                new Query<WareSkuEntity>().getPage(params),
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

        // 增量走 SQL 的 stock = stock + #{skuNum}。先查再算会丢并发更新，而且原来那句
        // set(stock, wareSkuEntity.getStock() + skuNum) 里的 getStock() 刚被赋成 skuNum，
        // 算出来是 2 倍采购量，不是"原库存 + 采购量"
        wareSkuDao.addStock(skuId, wareId, skuNum);
    }

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

    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean orderLockStock(WareSkuLockVo vo) {
        WareOrderTaskEntity wareOrderTaskEntity = new WareOrderTaskEntity();
        wareOrderTaskEntity.setOrderSn(vo.getOrderSn());
        wareOrderTaskEntity.setCreateTime(new Date());
        wareOrderTaskService.save(wareOrderTaskEntity);


        //1、按照下单的收货地址，找到一个就近仓库，锁定库存
        //2、找到每个商品在哪个仓库都有库存
        List<OrderItemVo> locks = vo.getLocks();

        List<SkuWareHasStock> collect = locks.stream().map((item) -> {
            SkuWareHasStock stock = new SkuWareHasStock();
            Long skuId = item.getSkuId();
            stock.setSkuId(skuId);
            stock.setNum(item.getCount());
            //查询这个商品在哪个仓库有库存
            List<Long> wareIdList = wareSkuDao.listWareIdHasSkuStock(skuId);
            stock.setWareId(wareIdList);

            return stock;
        }).toList();

        //2、锁定库存
        for (SkuWareHasStock hasStock : collect) {
            boolean skuStocked = false;
            Long skuId = hasStock.getSkuId();
            List<Long> wareIds = hasStock.getWareId();

            if (org.springframework.util.StringUtils.isEmpty(wareIds)) {
                //没有任何仓库有这个商品的库存
                throw new NoStockException(skuId);
            }

            //1、如果每一个商品都锁定成功,将当前商品锁定了几件的工作单记录发给MQ
            //2、锁定失败。前面保存的工作单信息都回滚了。发送出去的消息，即使要解锁库存，由于在数据库查不到指定的id，所有就不用解锁
            for (Long wareId : wareIds) {
                //锁定成功就返回1，失败就返回0
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
                    mqPublisher.publish(MqConstant.Exchanges.STOCK_EVENT, MqConstant.RoutingKeys.STOCK_LOCKED, lockedTo);
                    break;
                } else {
                    //当前仓库锁失败，重试下一个仓库
                }
            }

            if (!skuStocked) {
                //当前商品所有仓库都没有锁住
                throw new NoStockException(skuId);
            }
        }

        //3、肯定全部都是锁定成功的
        return true;
    }

    @Override
    public void unlockStock(StockLockedTo to) {
        //库存工作单的id
        StockDetailTo detail = to.getDetailTo();
        Long detailId = detail.getId();

        /**
         * 解锁
         * 1、查询数据库关于这个订单锁定库存信息
         *   有：证明库存锁定成功了
         *      解锁：订单状况
         *          1、没有这个订单，必须解锁库存
         *          2、有这个订单，不一定解锁库存
         *              订单状态：已取消：解锁库存
         *                      已支付：不能解锁库存
         */
        WareOrderTaskDetailEntity taskDetailInfo = wareOrderTaskDetailService.getById(detailId);
        if (taskDetailInfo != null) {
            //查出wms_ware_order_task工作单的信息
            Long id = to.getId();
            WareOrderTaskEntity orderTaskInfo = wareOrderTaskService.getById(id);
            //获取订单号查询订单状态
            String orderSn = orderTaskInfo.getOrderSn();
            //远程查询订单信息
            R<OrderVo> orderData = orderFeignService.getOrderStatus(orderSn);
            if (orderData.getCode() == 0) {
                //订单数据返回成功
                OrderVo orderInfo = orderData.getData();

                //判断订单状态是否已取消或者支付或者订单不存在
                if (orderInfo == null || orderInfo.getStatus() == 4) {
                    //订单已被取消，才能解锁库存
                    if (taskDetailInfo.getLockStatus() == 1) {
                        //当前库存工作单详情状态1，已锁定，但是未解锁才可以解锁
                        unLockStock(detail.getSkuId(),detail.getWareId(),detail.getSkuNum(),detailId);
                    }
                }
            } else {
                //消息拒绝以后重新放在队列里面，让别人继续消费解锁
                //远程调用服务失败
                throw new RuntimeException("远程调用服务失败");
            }
        } else {
            //无需解锁
        }
    }

    /**
     * 防止订单服务卡顿，导致订单状态消息一直改不了，库存优先到期，查订单状态新建，什么都不处理
     * 导致卡顿的订单，永远都不能解锁库存
     * @param orderTo
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void unlockStock(OrderTo orderTo) {

        String orderSn = orderTo.getOrderSn();
        //查一下最新的库存解锁状态，防止重复解锁库存
        WareOrderTaskEntity orderTaskEntity = wareOrderTaskService.getOrderTaskByOrderSn(orderSn);

        //按照工作单的id找到所有 没有解锁的库存，进行解锁
        Long id = orderTaskEntity.getId();
        List<WareOrderTaskDetailEntity> list = wareOrderTaskDetailService.list(new QueryWrapper<WareOrderTaskDetailEntity>()
                .eq("task_id", id).eq("lock_status", 1));

        for (WareOrderTaskDetailEntity taskDetailEntity : list) {
            unLockStock(taskDetailEntity.getSkuId(),
                    taskDetailEntity.getWareId(),
                    taskDetailEntity.getSkuNum(),
                    taskDetailEntity.getId());
        }

    }

    /**
     * 解锁库存的方法
     * @param skuId
     * @param wareId
     * @param num
     * @param taskDetailId
     */
    public void unLockStock(Long skuId,Long wareId,Integer num,Long taskDetailId) {

        //库存解锁
        wareSkuDao.unLockStock(skuId,wareId,num);

        //更新工作单的状态
        WareOrderTaskDetailEntity taskDetailEntity = new WareOrderTaskDetailEntity();
        taskDetailEntity.setId(taskDetailId);
        //变为已解锁
        taskDetailEntity.setLockStatus(2);
        wareOrderTaskDetailService.updateById(taskDetailEntity);

    }


    @Override
    public List<SkuDeleteBlockerTo> canDelete(List<Long> skuIds) {
        List<Long> ids = skuIds == null ? List.of()
                : skuIds.stream().filter(Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) {
            return List.of();
        }

        // 有量才算占用：stock 为 0 的行只是"这个仓库曾经放过这个 sku"，删商品时顺手清掉就行
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

    /** 手工装 map 而不是 Collectors.toMap：后者遇到 null 的 value 会直接抛 NPE */
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

    @Data
    static
    class SkuWareHasStock {
        private Long skuId;
        private Integer num;
        private List<Long> wareId;
    }

}