package search.listener;

import com.rabbitmq.client.Channel;
import common.mq.MqConstant;
import common.mq.MqPublisher;
import common.to.mq.ProductDownTo;
import common.utils.MqRetryUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import search.service.ProductSaveService;

import java.io.IOException;

/**
 * 消费 {@code product.down}，把下架商品从 ES 里清掉。失败走 DLX → 重试队列 → 最多 3 次 → DLQ。
 */
@Slf4j
@Component
@RabbitListener(queues = MqConstant.Queues.SEARCH_PRODUCT_DOWN)
public class ProductDownListener {

    private static final int MAX_RETRY = 3;

    private final ProductSaveService productSaveService;
    private final MqPublisher mqPublisher;

    public ProductDownListener(ProductSaveService productSaveService, MqPublisher mqPublisher) {
        this.productSaveService = productSaveService;
        this.mqPublisher = mqPublisher;
    }

    @RabbitHandler
    public void onProductDown(ProductDownTo to, Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        try {
            productSaveService.productStatusDown(to.getSpuIds());
            channel.basicAck(deliveryTag, false);
            log.info("下架商品已从 ES 清除：spuIds={}", to.getSpuIds());
        } catch (Exception e) {
            int retried = MqRetryUtils.attemptCount(message, MqConstant.Queues.SEARCH_PRODUCT_DOWN);
            if (retried >= MAX_RETRY) {
                mqPublisher.publish(MqConstant.Exchanges.SEARCH_PRODUCT_DOWN_DLX,
                        MqConstant.Queues.SEARCH_PRODUCT_DOWN_DLQ, to);
                channel.basicAck(deliveryTag, false);
                log.error("下架商品从 ES 清除重试 {} 次仍失败，已投入死信队列 {}：spuIds={}",
                        retried, MqConstant.Queues.SEARCH_PRODUCT_DOWN_DLQ, to.getSpuIds(), e);
            } else {
                channel.basicNack(deliveryTag, false, false);
                log.warn("下架商品从 ES 清除失败，交由重试队列延迟重投（已重试 {} 次）：spuIds={}",
                        retried, to.getSpuIds(), e);
            }
        }
    }
}
