package thirdParty.listener;

import com.rabbitmq.client.Channel;
import common.mq.MqConstant;
import common.to.mq.ProductDeletedTo;
import common.utils.MqRetryUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import thirdParty.service.MediaService;

import java.io.IOException;
import java.util.List;

/**
 * 消费 {@code product.deleted}，删掉 MinIO 里这个商品引用过的文件。
 *
 * <p>失败处理和 coupon 侧一致：nack（requeue=false）→ DLX → 重试队列延迟 1 分钟 → 重投，
 * 重试满 {@value #MAX_RETRY} 次仍失败则投进死信队列。</p>
 *
 * <p><b>为什么 {@code failed} 非空必须抛异常：</b>{@code MediaService.deleteBatch} 是
 * "单个失败不中断"的尽力而为语义，返回的是没删掉的地址清单。如果这里只打日志就 ack，
 * 那些文件就<b>永久孤儿</b>了 —— 消息已经被确认，没有任何机制会再回来处理它们。</p>
 *
 * <p>代价是：一个永远删不掉的地址（比如管理员手填的外站 URL，{@code resolveKey} 认不出来）
 * 会走满重试次数然后进死信队列，而不是自动消失。这是刻意的 —— 让它暴露给人看，
 * 比静默留在桶里强。</p>
 */
@Slf4j
@Component
@RabbitListener(queues = MqConstant.Queues.THIRDPARTY_PRODUCT_DELETED)
public class ProductDeletedListener {

    /** 最多重试几次（不含首次投递） */
    private static final int MAX_RETRY = 3;

    private final MediaService mediaService;
    private final RabbitTemplate rabbitTemplate;

    public ProductDeletedListener(MediaService mediaService, RabbitTemplate rabbitTemplate) {
        this.mediaService = mediaService;
        this.rabbitTemplate = rabbitTemplate;
    }

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
                rabbitTemplate.convertAndSend(MqConstant.Exchanges.THIRDPARTY_PRODUCT_DELETED_DLX,
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
