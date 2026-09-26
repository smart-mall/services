package ware.listener;

import com.rabbitmq.client.Channel;
import common.mq.MqConstant;
import common.mq.MqPublisher;
import common.to.mq.ProductDeletedTo;
import common.utils.MqRetryUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import ware.service.WareSkuService;

import java.io.IOException;

/**
 * 消费 {@code product.deleted}（队列 {@code ware.product.deleted.queue}），清掉已删 sku 的零库存行。
 *
 * <p>消息体 {@link ProductDeletedTo} 由 product 删除商品成功后在事务内落 outbox 发出，必须晚于删除本身：
 * 跨服务删除不可回滚，商品没删掉就不能清库存行，否则会出现"库存行没了、商品还在"。</p>
 *
 * <p>失败处理走三段式：{@code basicNack(requeue=false)} → 队列 DLX → 重试队列躺 1 分钟 → 回到本队列，
 * 重试满 3 次仍失败则投死信队列等人工处理。不用 {@code requeue=true}，那等于全速无限重投，没有退避与上限。</p>
 */
@Slf4j
@Component
@RabbitListener(queues = MqConstant.Queues.WARE_PRODUCT_DELETED)
public class ProductDeletedListener {

    /** 最多重试几次（不含首次投递） */
    private static final int MAX_RETRY = 3;

    private final WareSkuService wareSkuService;
    private final MqPublisher mqPublisher;

    public ProductDeletedListener(WareSkuService wareSkuService, MqPublisher mqPublisher) {
        this.wareSkuService = wareSkuService;
        this.mqPublisher = mqPublisher;
    }

    /**
     * 消费一条商品删除事件，清理这些 SKU 的零库存行。
     *
     * <p>成功即 ack；失败按已重试次数分流：未到上限则 nack 交给重试队列延迟重投，到上限则投死信队列后 ack。</p>
     *
     * @param to      商品删除事件，含被删商品下的全部 SKU ID
     * @param message 当前消息，从中取投递标签与 {@code x-death} 重试次数
     * @param channel 消费通道，用于 ack / nack
     * @throws IOException ack / nack 与 broker 通信失败时抛出
     */
    @RabbitHandler
    public void onProductDeleted(ProductDeletedTo to, Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        try {
            int removed = wareSkuService.deleteZeroStock(to.getSkuIds());
            channel.basicAck(deliveryTag, false);
            log.info("清理商品库存行完成：skuIds={}，删除 {} 行", to.getSkuIds(), removed);
        } catch (Exception e) {
            int retried = MqRetryUtils.attemptCount(message, MqConstant.Queues.WARE_PRODUCT_DELETED);
            if (retried >= MAX_RETRY) {
                mqPublisher.publish(MqConstant.Exchanges.WARE_PRODUCT_DELETED_DLX,
                        MqConstant.Queues.WARE_PRODUCT_DELETED_DLQ, to);
                // 已经落到死信队列，必须 ack 掉原消息；不 ack 会被重新投递，同一条消息反复进死信
                channel.basicAck(deliveryTag, false);
                log.error("清理商品库存行重试 {} 次仍失败，已投入死信队列 {}：skuIds={}",
                        retried, MqConstant.Queues.WARE_PRODUCT_DELETED_DLQ, to.getSkuIds(), e);
            } else {
                // requeue=false：不走"塞回队头"，而是交给队列自己的 DLX → 重试队列延迟 1 分钟
                channel.basicNack(deliveryTag, false, false);
                log.warn("清理商品库存行失败，交由重试队列延迟重投（已重试 {} 次）：skuIds={}",
                        retried, to.getSkuIds(), e);
            }
        }
    }
}
