package thirdParty.config;

import common.mq.MqBuilder;
import common.mq.MqConstant;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * third-party 侧消费 {@code product.deleted} 所需的交换机、队列与绑定，用于清理 MinIO 中的孤儿文件。
 *
 * <p>拓扑为业务队列 → DLX → TTL 重试队列 → 回到业务交换机，重试到上限由监听器投进死信队列。
 * 每个消费方各持一个业务队列，同一条消息被各服务独立消费，互不阻塞。</p>
 */
@Configuration
public class RabbitMQConfig {

    /**
     * 商品事件交换机，topic 类型，商品域的所有事件都投到这里。
     *
     * @return 持久化的 topic 交换机
     */
    @Bean
    public Exchange productExchange() {
        return MqBuilder.topicExchange(MqConstant.Exchanges.PRODUCT);
    }

    /**
     * 业务队列，消费失败 nack 后由自己的 DLX 接走。
     *
     * @return 绑定了死信交换机的业务队列
     */
    @Bean
    public Queue thirdPartyProductDeletedQueue() {
        return MqBuilder.deadLetterQueue(
                MqConstant.Queues.THIRDPARTY_PRODUCT_DELETED,
                MqConstant.Exchanges.THIRDPARTY_DLX,
                MqConstant.RoutingKeys.THIRDPARTY_PRODUCT_DELETED_RETRY);
    }

    /**
     * 把业务队列绑到商品事件交换机上，只接收路由键 {@code product.deleted} 的消息。
     *
     * @return 队列与交换机的绑定
     */
    @Bean
    public Binding thirdPartyProductDeletedBinding() {
        return MqBuilder.bind(
                MqConstant.Queues.THIRDPARTY_PRODUCT_DELETED,
                MqConstant.Exchanges.PRODUCT,
                MqConstant.RoutingKeys.PRODUCT_DELETED);
    }

    /**
     * 业务队列的死信交换机，direct 类型。
     *
     * @return 持久化的 direct 交换机
     */
    @Bean
    public Exchange thirdPartyDlxExchange() {
        return MqBuilder.directExchange(MqConstant.Exchanges.THIRDPARTY_DLX);
    }

    /**
     * 重试队列，消息在此滞留 1 分钟后死信回业务交换机，等价于延迟重投。
     *
     * @return 带 TTL 的重试队列
     */
    @Bean
    public Queue thirdPartyProductDeletedRetryQueue() {
        return MqBuilder.ttlQueue(
                MqConstant.Queues.THIRDPARTY_PRODUCT_DELETED_RETRY,
                MqConstant.Exchanges.PRODUCT,
                MqConstant.RoutingKeys.PRODUCT_DELETED,
                MqConstant.TtlMillis.PRODUCT_DELETED_RETRY);
    }

    /**
     * 把重试队列绑到 DLX 上，路由键与重试队列同名。
     *
     * @return 重试队列与 DLX 的绑定
     */
    @Bean
    public Binding thirdPartyProductDeletedRetryBinding() {
        return MqBuilder.bind(
                MqConstant.Queues.THIRDPARTY_PRODUCT_DELETED_RETRY,
                MqConstant.Exchanges.THIRDPARTY_DLX,
                MqConstant.RoutingKeys.THIRDPARTY_PRODUCT_DELETED_RETRY);
    }

    /**
     * 死信队列，没有消费者，堆积的消息等人工处理。
     *
     * @return 持久化死信队列
     */
    @Bean
    public Queue thirdPartyProductDeletedDlq() {
        return MqBuilder.durableQueue(MqConstant.Queues.THIRDPARTY_PRODUCT_DELETED_DLQ);
    }

    /**
     * 把死信队列绑到 DLX 上，路由键与队列名同值。
     *
     * @return 死信队列与 DLX 的绑定
     */
    @Bean
    public Binding thirdPartyProductDeletedDlqBinding() {
        return MqBuilder.bind(
                MqConstant.Queues.THIRDPARTY_PRODUCT_DELETED_DLQ,
                MqConstant.Exchanges.THIRDPARTY_DLX,
                MqConstant.RoutingKeys.THIRDPARTY_PRODUCT_DELETED_DLQ);
    }
}
