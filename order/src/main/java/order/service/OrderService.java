package order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import common.to.mq.SeckillOrderTo;
import common.utils.PageUtils;
import order.entity.OrderEntity;
import order.vo.*;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * 订单
 *
 * @author ??
 * @email sunlightcs@gmail.com
 * @date 2025-09-15 11:22:13
 */
public interface OrderService extends IService<OrderEntity> {

    PageUtils queryPage(Map<String, Object> params);

    OrderConfirmVo confirmOrder() throws ExecutionException, InterruptedException;

    SubmitOrderResponseVo submitOrder(OrderSubmitVo vo);

    OrderEntity getOrderByOrderSn(String orderSn);

    void closeOrder(OrderEntity orderEntity);

    PayVo getOrderPay(String orderSn);

    PageUtils queryPageWithItem(Map<String, Object> params);

    @Transactional(rollbackFor = Exception.class)
    String handlePayResult(PayAsyncVo asyncVo);

    String asyncNotify(String notifyData);

    void createSeckillOrder(SeckillOrderTo orderTo);
}

