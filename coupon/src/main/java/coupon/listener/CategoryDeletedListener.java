package coupon.listener;

import com.rabbitmq.client.Channel;
import common.mq.MqConstant;
import common.mq.MqPublisher;
import common.to.mq.CategoryDeletedTo;
import common.utils.MqRetryUtils;
import coupon.service.ProductCleanupService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 分类删除事件的消费者：清掉这些分类在 coupon 侧的失效券范围行。
 *
 * <p>监听 {@code coupon.category.deleted.queue}，消息体是 {@link CategoryDeletedTo}，由 product 在
 * 分类行删除后投递；分类已不存在，只能用消息里带的 ID，不能回查 product。
 *
 * <p>失败 nack 后由队列的 DLX 转入重试队列躺 1 分钟再回到本队列，满 {@value #MAX_RETRY} 次仍失败则投死信队列。
 */
@Slf4j
@Component
@RabbitListener(queues = MqConstant.Queues.COUPON_CATEGORY_DELETED)
public class CategoryDeletedListener {

    /** 最多重试几次（不含首次投递）；4 次都失败基本不是抖动，而是数据或代码问题。 */
    private static final int MAX_RETRY = 3;

    private final ProductCleanupService productCleanupService;
    private final MqPublisher mqPublisher;

    /**
     * 创建分类删除事件监听器。
     *
     * @param productCleanupService 优惠数据清理服务
     * @param mqPublisher 消息发布器，重试到上限时用来投死信队列
     */
    public CategoryDeletedListener(ProductCleanupService productCleanupService, MqPublisher mqPublisher) {
        this.productCleanupService = productCleanupService;
        this.mqPublisher = mqPublisher;
    }

    /**
     * 消费一条分类删除事件，清理这些分类在 coupon 侧的失效券范围行。
     *
     * <p>清理成功即 ack；失败且未到重试上限时 {@code basicNack(requeue=false)}，由队列的 DLX 接走延迟重投。
     *
     * @param to 分类删除事件，{@code catIds} 为待清理的分类主键（含子分类）
     * @param message 当前消息，已重试次数从消息头的 {@code x-death} 里读
     * @param channel 消费通道，用于 ack / nack
     * @throws IOException ack / nack 与 broker 通信失败时抛出
     */
    @RabbitHandler
    public void onCategoryDeleted(CategoryDeletedTo to, Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        try {
            productCleanupService.cleanupCategory(to.getCatIds());
            channel.basicAck(deliveryTag, false);
            log.info("清理分类优惠数据完成：catIds={}", to.getCatIds());
        } catch (Exception e) {
            // 次数由 broker 的 x-death 头给出，不靠消费端内存计数，服务重启也不会丢
            int retried = MqRetryUtils.attemptCount(message, MqConstant.Queues.COUPON_CATEGORY_DELETED);
            if (retried >= MAX_RETRY) {
                // 到上限：投死信队列让人看见后再 ack 收尾；此处若 nack 会被无限重投回来
                mqPublisher.publish(MqConstant.Exchanges.COUPON_DLX,
                        MqConstant.RoutingKeys.COUPON_CATEGORY_DELETED_DLQ, to);
                channel.basicAck(deliveryTag, false);
                log.error("清理分类优惠数据重试 {} 次仍失败，已投入死信队列 {}：catIds={}",
                        retried, MqConstant.Queues.COUPON_CATEGORY_DELETED_DLQ, to.getCatIds(), e);
            } else {
                // requeue=false：不塞回队头，改由队列的 DLX 送进重试队列躺 1 分钟
                channel.basicNack(deliveryTag, false, false);
                log.warn("清理分类优惠数据失败，交由重试队列延迟重投（已重试 {} 次）：catIds={}",
                        retried, to.getCatIds(), e);
            }
        }
    }
}
