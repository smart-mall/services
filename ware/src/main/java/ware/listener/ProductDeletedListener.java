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
 * 消费 {@code product.deleted}，清掉已删 sku 的零库存行。
 *
 * <p>为什么走消息而不是让 product 同步调一次删除：这条清理必须发生在商品删除<b>成功之后</b>，
 * 而跨服务的删除不可回滚。消息和 product 那 7 张表的删除在同一个事务里落 outbox，
 * 商品删失败就不会发消息，也就不会出现"库存行没了、商品还在"。</p>
 *
 * <p>失败处理照 coupon / third-party 那套三段式，不照抄本服务 StockReleaseListener 的
 * {@code basicReject(requeue=true)} —— 那种写法没有退避、没有上限、没有出口。</p>
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
                channel.basicAck(deliveryTag, false);
                log.error("清理商品库存行重试 {} 次仍失败，已投入死信队列 {}：skuIds={}",
                        retried, MqConstant.Queues.WARE_PRODUCT_DELETED_DLQ, to.getSkuIds(), e);
            } else {
                // requeue=false：交给队列自己的 DLX → 重试队列延迟 1 分钟
                channel.basicNack(deliveryTag, false, false);
                log.warn("清理商品库存行失败，交由重试队列延迟重投（已重试 {} 次）：skuIds={}",
                        retried, to.getSkuIds(), e);
            }
        }
    }
}
