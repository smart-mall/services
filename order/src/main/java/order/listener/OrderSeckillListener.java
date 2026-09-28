package order.listener;

import com.rabbitmq.client.Channel;
import common.mq.MqConstant;
import common.to.mq.SeckillOrderTo;
import lombok.extern.slf4j.Slf4j;
import order.service.OrderService;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;


/**
 * 秒杀建单监听器：消费 {@code order.seckill-created.queue}，把秒杀成功的消息落成真实订单。
 *
 * <p>消息体 {@link SeckillOrderTo} 由 seckill 服务在扣减秒杀库存成功后投递；监听器只负责建单落库，
 * 不再校验库存。
 */
@Slf4j
@Component
@RabbitListener(queues = MqConstant.Queues.ORDER_SECKILL_CREATED)
public class OrderSeckillListener {

    @Autowired
    private OrderService orderService;

    /**
     * 消费秒杀建单消息，调用 {@link OrderService#createSeckillOrder} 落库。
     *
     * <p>消息体 {@link SeckillOrderTo} 自带 orderSn，建单幂等性依赖该字段。处理成功即 ack；任何异常都
     * {@code basicReject(requeue=true)} 重新入队，队列没有重试上限与死信兜底，不可重试的失败
     * （如收货地址缺失）会在这里无限循环。
     *
     * @param orderTo 秒杀订单消息体，含 orderSn、memberId、skuId、num 与秒杀价
     * @param channel RabbitMQ 信道，用于回执
     * @param message 原始消息，取 deliveryTag 做 ack / reject
     * @throws IOException 回执发送失败时抛出
     */
    @RabbitHandler
    public void listener(SeckillOrderTo orderTo, Channel channel, Message message) throws IOException {

        log.info("准备创建秒杀单的详细信息...");

        try {
            orderService.createSeckillOrder(orderTo);
            channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
        } catch (Exception e) {
            channel.basicReject(message.getMessageProperties().getDeliveryTag(),true);
        }

    }

}
