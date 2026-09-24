package coupon.listener;

import com.rabbitmq.client.Channel;
import common.mq.MqConstant;
import common.mq.MqPublisher;
import common.to.mq.ProductDeletedTo;
import common.utils.MqRetryUtils;
import coupon.service.ProductCleanupService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 消费 {@code product.deleted}，清掉这个商品在 coupon 侧的优惠数据。
 *
 * <p>失败处理刻意和项目里现有的三个监听器不同：它们用的是
 * {@code basicReject(tag, true)}（requeue=true），那是"全速无限重投"—— 没有退避、
 * 没有上限、没有出口，一条毒消息能把消费者打死且完全静默。这里改成：</p>
 *
 * <pre>
 * 失败 → basicNack(requeue=false) → 队列的 DLX → 重试队列（躺 1 分钟）→ 回到本队列
 * 重试满 3 次仍失败 → 投到死信队列，人工处理
 * </pre>
 */
@Slf4j
@Component
@RabbitListener(queues = MqConstant.Queues.COUPON_PRODUCT_DELETED)
public class ProductDeletedListener {

    /** 最多重试几次（不含首次投递）。4 次都失败基本就不是抖动，而是数据或代码问题 */
    private static final int MAX_RETRY = 3;

    private final ProductCleanupService productCleanupService;
    private final MqPublisher mqPublisher;

    public ProductDeletedListener(ProductCleanupService productCleanupService, MqPublisher mqPublisher) {
        this.productCleanupService = productCleanupService;
        this.mqPublisher = mqPublisher;
    }

    @RabbitHandler
    public void onProductDeleted(ProductDeletedTo to, Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        try {
            productCleanupService.cleanupProduct(to.getSpuIds(), to.getSkuIds());
            channel.basicAck(deliveryTag, false);
            log.info("清理商品优惠数据完成：spuIds={}，skuIds={}", to.getSpuIds(), to.getSkuIds());
        } catch (Exception e) {
            int retried = MqRetryUtils.attemptCount(message, MqConstant.Queues.COUPON_PRODUCT_DELETED);
            if (retried >= MAX_RETRY) {
                // 到上限了：投到死信队列让人看见，别再无限打转
                mqPublisher.publish(MqConstant.Exchanges.COUPON_PRODUCT_DELETED_DLX,
                        MqConstant.Queues.COUPON_PRODUCT_DELETED_DLQ, to);
                channel.basicAck(deliveryTag, false);
                log.error("清理商品优惠数据重试 {} 次仍失败，已投入死信队列 {}：spuIds={}",
                        retried, MqConstant.Queues.COUPON_PRODUCT_DELETED_DLQ, to.getSpuIds(), e);
            } else {
                // requeue=false：不走"塞回队头"，而是交给队列自己的 DLX → 重试队列延迟 1 分钟
                channel.basicNack(deliveryTag, false, false);
                log.warn("清理商品优惠数据失败，交由重试队列延迟重投（已重试 {} 次）：spuIds={}",
                        retried, to.getSpuIds(), e);
            }
        }
    }
}
