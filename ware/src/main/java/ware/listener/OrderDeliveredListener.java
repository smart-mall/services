package ware.listener;

import com.rabbitmq.client.Channel;
import common.mq.MqConstant;
import common.mq.MqPublisher;
import common.to.mq.OrderDeliveredTo;
import common.utils.MqRetryUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import ware.service.WareSkuService;

import java.io.IOException;

/**
 * 消费 {@code order.delivered}（队列 {@code ware.order.delivered.queue}），把订单锁定的库存实扣掉。
 *
 * <p>消息体 {@link OrderDeliveredTo} 由 order 发货成功后在事务内落 outbox 发出，必须晚于状态变更：
 * 先发消息再改状态的话，改状态失败会留下"货已出、单还没发货"，而 outbox 正是靠与业务写同事务
 * 来避免这件事。</p>
 *
 * <p>失败处理走三段式：{@code basicNack(requeue=false)} → 队列 DLX → 重试队列躺 1 分钟 → 回到本队列，
 * 重试满 3 次仍失败则投死信队列等人工处理。不用 {@code requeue=true}，那等于全速无限重投，没有退避与上限。</p>
 *
 * <p><b>唯一不进重试的失败是"明细与库存行对不上"</b>：那是代码逻辑错误而不是依赖抖动，重试不会让它变好，
 * 扣减服务自己按实际锁定数扣完并记 error，本监听器照常 ack。</p>
 */
@Slf4j
@Component
@RabbitListener(queues = MqConstant.Queues.WARE_ORDER_DELIVERED)
public class OrderDeliveredListener {

    /** 最多重试几次（不含首次投递）。 */
    private static final int MAX_RETRY = 3;

    private final WareSkuService wareSkuService;
    private final MqPublisher mqPublisher;

    public OrderDeliveredListener(WareSkuService wareSkuService, MqPublisher mqPublisher) {
        this.wareSkuService = wareSkuService;
        this.mqPublisher = mqPublisher;
    }

    /**
     * 消费一条订单发货事件，实扣该订单锁定的库存。
     *
     * <p>成功即 ack；失败按已重试次数分流：未到上限则 nack 交给重试队列延迟重投，到上限则投死信队列后 ack。</p>
     *
     * @param to      订单发货事件，含订单号
     * @param message 当前消息，从中取投递标签与 {@code x-death} 重试次数
     * @param channel 消费通道，用于 ack / nack
     * @throws IOException ack / nack 与 broker 通信失败时抛出
     */
    @RabbitHandler
    public void onOrderDelivered(OrderDeliveredTo to, Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        try {
            wareSkuService.deductStock(to);
            channel.basicAck(deliveryTag, false);
            log.info("订单发货实扣库存完成：orderSn={}", to.getOrderSn());
        } catch (Exception e) {
            int retried = MqRetryUtils.attemptCount(message, MqConstant.Queues.WARE_ORDER_DELIVERED);
            if (retried >= MAX_RETRY) {
                mqPublisher.publish(MqConstant.Exchanges.WARE_DLX,
                        MqConstant.RoutingKeys.WARE_ORDER_DELIVERED_DLQ, to);
                // 已经落到死信队列，必须 ack 掉原消息；不 ack 会被重新投递，同一条消息反复进死信
                channel.basicAck(deliveryTag, false);
                log.error("订单发货实扣库存重试 {} 次仍失败，已投入死信队列 {}：orderSn={}",
                        retried, MqConstant.Queues.WARE_ORDER_DELIVERED_DLQ, to.getOrderSn(), e);
            } else {
                // requeue=false：不走"塞回队头"，而是交给队列自己的 DLX → 重试队列延迟 1 分钟
                channel.basicNack(deliveryTag, false, false);
                log.warn("订单发货实扣库存失败，交由重试队列延迟重投（已重试 {} 次）：orderSn={}",
                        retried, to.getOrderSn(), e);
            }
        }
    }
}
