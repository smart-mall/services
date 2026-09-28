package order.listener;

import com.rabbitmq.client.Channel;
import common.mq.MqConstant;
import order.entity.OrderEntity;
import order.service.OrderService;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;

/**
 * 超时关单监听器：消费 {@code order.timed-out.queue}，关闭延迟队列死信出来的订单。
 *
 * <p>本类无状态。队列没有重试上限与死信兜底，处理失败会把消息放回队列一直重投。</p>
 */
@RabbitListener(queues = MqConstant.Queues.ORDER_TIMED_OUT)
@Service
public class OrderCloseListener {

    @Autowired
    private OrderService orderService;

    /**
     * 消费过期订单消息，调用 {@link OrderService#closeOrder} 关单。
     *
     * <p>消息来自 {@code order.created.delay.queue} 的 TTL 死信，消息体是下单时投递的
     * {@link OrderEntity} 快照。处理成功即 ack；任何异常都 {@code basicReject(requeue=true)}
     * 重新入队，会一直重投到关单不再抛异常为止。</p>
     *
     * @param orderEntity 过期订单快照，至少带 orderSn
     * @param channel RabbitMQ 信道，用于回执
     * @param message 原始消息，取 deliveryTag 做 ack / reject
     * @throws IOException 回执发送失败时抛出
     */
    @RabbitHandler
    public void listener(OrderEntity orderEntity, Channel channel, Message message) throws IOException {
        System.out.println("收到过期的订单信息，准备关闭订单" + orderEntity.getOrderSn());
        try {
            orderService.closeOrder(orderEntity);
            channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
        } catch (Exception e) {
            channel.basicReject(message.getMessageProperties().getDeliveryTag(),true);
        }

    }

}
