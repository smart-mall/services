package thirdParty.listener;

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
import thirdParty.service.MediaService;

import java.io.IOException;
import java.util.List;

/**
 * 消费 {@code product.deleted}，删除该商品在 MinIO 中引用过的文件。
 *
 * <p>失败处理：nack（requeue=false）→ DLX → 重试队列延迟 1 分钟 → 重投；重试满
 * {@value #MAX_RETRY} 次仍失败则投进死信队列。</p>
 *
 * <p>{@code failed} 非空必须抛异常而不能 ack：{@code MediaService.deleteBatch} 是"单个失败不中断"
 * 的尽力而为语义，返回的是没删掉的地址，一旦 ack 这些文件就永久成为孤儿，没有任何机制会再处理它们。
 * 代价是永远删不掉的地址（如管理员手填的外站 URL，{@code resolveKey} 认不出来）会走满重试后进
 * 死信队列，这是刻意让它暴露给人看，而不是静默留在桶里。</p>
 */
@Slf4j
@Component
@RabbitListener(queues = MqConstant.Queues.THIRDPARTY_PRODUCT_DELETED)
public class ProductDeletedListener {

    /** 最多重试几次（不含首次投递）。 */
    private static final int MAX_RETRY = 3;

    private final MediaService mediaService;
    private final MqPublisher mqPublisher;

    public ProductDeletedListener(MediaService mediaService, MqPublisher mqPublisher) {
        this.mediaService = mediaService;
        this.mqPublisher = mqPublisher;
    }

    /**
     * 处理商品删除事件，清理消息中携带的全部图片地址。
     *
     * <p>清理不完整时不确认消息，交由 DLX 重试；达到上限后转入死信队列并确认消息。
     *
     * @param to      商品删除消息，{@code imageUrls} 可为 {@code null}
     * @param message 原始消息，用于取 deliveryTag 与重试次数
     * @param channel 消费通道，用于 ack 与 nack
     * @throws IOException 与 broker 通信失败时抛出
     */
    @RabbitHandler
    public void onProductDeleted(ProductDeletedTo to, Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        try {
            List<String> failed = mediaService.deleteBatch(to.getImageUrls());
            if (!failed.isEmpty()) {
                // 抛出去交给下面的重试/死信逻辑，绝不能在这里 ack
                throw new IllegalStateException(failed.size() + " 个文件没能删除：" + failed);
            }

            channel.basicAck(deliveryTag, false);
            log.info("清理商品图片完成：spuIds={}，文件数={}", to.getSpuIds(),
                    to.getImageUrls() == null ? 0 : to.getImageUrls().size());
        } catch (Exception e) {
            int retried = MqRetryUtils.attemptCount(message, MqConstant.Queues.THIRDPARTY_PRODUCT_DELETED);
            if (retried >= MAX_RETRY) {
                mqPublisher.publish(MqConstant.Exchanges.THIRDPARTY_PRODUCT_DELETED_DLX,
                        MqConstant.Queues.THIRDPARTY_PRODUCT_DELETED_DLQ, to);
                channel.basicAck(deliveryTag, false);
                log.error("清理商品图片重试 {} 次仍失败，已投入死信队列 {}：spuIds={}",
                        retried, MqConstant.Queues.THIRDPARTY_PRODUCT_DELETED_DLQ, to.getSpuIds(), e);
            } else {
                channel.basicNack(deliveryTag, false, false);
                log.warn("清理商品图片失败，交由重试队列延迟重投（已重试 {} 次）：spuIds={}",
                        retried, to.getSpuIds(), e);
            }
        }
    }
}
