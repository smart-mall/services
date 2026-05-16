package ware.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import common.exception.NoStockException;
import common.to.OrderTo;
import common.to.mq.StockDetailTo;
import common.to.mq.StockLockedTo;
import common.utils.PageUtils;
import common.utils.Query;
import common.utils.R;
import lombok.Data;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ware.dao.WareSkuDao;
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
import java.util.List;
import java.util.Map;


@Service("wareSkuService")
public class WareSkuServiceImpl extends ServiceImpl<WareSkuDao, WareSkuEntity> implements WareSkuService {

    private final WareSkuDao wareSkuDao;
    private final ProductFeignService productFeignService;
    private final RabbitTemplate rabbitTemplate;
    private final WareOrderTaskService wareOrderTaskService;
    private final WareOrderTaskDetailService wareOrderTaskDetailService;
    private final OrderFeignService orderFeignService;
    private final WareInfoService wareInfoService;

    public WareSkuServiceImpl(WareSkuDao wareSkuDao, ProductFeignService productFeignService, RabbitTemplate rabbitTemplate, WareOrderTaskService wareOrderTaskService, WareOrderTaskDetailService wareOrderTaskDetailService, OrderFeignService orderFeignService, WareInfoService wareInfoService) {
        this.wareSkuDao = wareSkuDao;
        this.productFeignService = productFeignService;
        this.rabbitTemplate = rabbitTemplate;
        this.wareOrderTaskService = wareOrderTaskService;
        this.wareOrderTaskDetailService = wareOrderTaskDetailService;
        this.orderFeignService = orderFeignService;
        this.wareInfoService = wareInfoService;
    }

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
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

        page.getRecords().forEach(wareSkuEntity -> {
            wareSkuEntity.setWareName(wareInfoEntities.stream().filter(wareInfoEntity -> wareInfoEntity.getId().equals(wareSkuEntity.getWareId())).findFirst().get().getName());
        });

        return new PageUtils(page);
    }

    @Override
    @Transactional
    public void addStock(Long skuId, Long wareId, Integer skuNum) {
        WareSkuEntity wareSkuEntity = new WareSkuEntity();
        wareSkuEntity.setSkuId(skuId);
        wareSkuEntity.setWareId(wareId);
        wareSkuEntity.setStock(skuNum);
        wareSkuEntity.setStockLocked(0);
        try {
            R info = productFeignService.getProduct(skuId);

            @SuppressWarnings("unchecked")
            Map<String, Object> skuInfo =  (Map<String, Object>) info.get("skuInfo");

            if (info.getCode() == 0) {
                wareSkuEntity.setSkuName((String) skuInfo.get("skuName"));
            }
        } catch (Exception ignored) {}


        LambdaQueryWrapper<WareSkuEntity> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WareSkuEntity::getSkuId, skuId)
                .eq(WareSkuEntity::getWareId, wareId);

        List<WareSkuEntity> wareSkuEntities = baseMapper.selectList(queryWrapper);
        if (wareSkuEntities == null || wareSkuEntities.isEmpty()) {
            baseMapper.insert(wareSkuEntity);
        } else {
            LambdaUpdateWrapper<WareSkuEntity> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(WareSkuEntity::getSkuId, skuId)
                    .eq(WareSkuEntity::getWareId, wareId)
                    .set(WareSkuEntity::getStock, wareSkuEntity.getStock() + skuNum);
            baseMapper.update(wareSkuEntity, updateWrapper);
        }

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
                    rabbitTemplate.convertAndSend("stock-event-exchange","stock.locked",lockedTo);
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
            R orderData = orderFeignService.getOrderStatus(orderSn);
            if (orderData.getCode() == 0) {
                //订单数据返回成功
                OrderVo orderInfo = orderData.getData("data", new TypeReference<OrderVo>() {});

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


    @Data
    static
    class SkuWareHasStock {
        private Long skuId;
        private Integer num;
        private List<Long> wareId;
    }

}