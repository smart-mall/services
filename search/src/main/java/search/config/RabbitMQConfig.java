package search.config;

import common.mq.MqBuilder;
import common.mq.MqConstant;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * search 侧消费 {@code product.down} 所需的交换机与队列，结构与 coupon / third-party 那两套一致：
 * 业务队列失败 → DLX → 重试队列躺 TTL → 回到业务队列；重试到上限由监听器投进 DLQ。
 */
@Configuration
public class RabbitMQConfig {

    @Bean
    public Exchange productEventExchange() {
        return MqBuilder.topicExchange(MqConstant.Exchanges.PRODUCT_EVENT);
    }

    @Bean
    public Queue searchProductDownQueue() {
        return MqBuilder.deadLetterQueue(
                MqConstant.Queues.SEARCH_PRODUCT_DOWN,
                MqConstant.Exchanges.SEARCH_PRODUCT_DOWN_DLX,
                MqConstant.RoutingKeys.SEARCH_PRODUCT_DOWN_RETRY);
    }

    @Bean
    public Binding searchProductDownBinding() {
        return MqBuilder.bind(
                MqConstant.Queues.SEARCH_PRODUCT_DOWN,
                MqConstant.Exchanges.PRODUCT_EVENT,
                MqConstant.RoutingKeys.PRODUCT_DOWN);
    }

    @Bean
    public Exchange searchProductDownDlx() {
        return MqBuilder.directExchange(MqConstant.Exchanges.SEARCH_PRODUCT_DOWN_DLX);
    }

    @Bean
    public Queue searchProductDownRetryQueue() {
        return MqBuilder.ttlQueue(
                MqConstant.Queues.SEARCH_PRODUCT_DOWN_RETRY,
                MqConstant.Exchanges.PRODUCT_EVENT,
                MqConstant.RoutingKeys.PRODUCT_DOWN,
                MqConstant.TtlMillis.PRODUCT_DOWN_RETRY);
    }

    @Bean
    public Binding searchProductDownRetryBinding() {
        return MqBuilder.bind(
                MqConstant.Queues.SEARCH_PRODUCT_DOWN_RETRY,
                MqConstant.Exchanges.SEARCH_PRODUCT_DOWN_DLX,
                MqConstant.RoutingKeys.SEARCH_PRODUCT_DOWN_RETRY);
    }

    @Bean
    public Queue searchProductDownDlq() {
        return MqBuilder.durableQueue(MqConstant.Queues.SEARCH_PRODUCT_DOWN_DLQ);
    }

    @Bean
    public Binding searchProductDownDlqBinding() {
        return MqBuilder.bind(
                MqConstant.Queues.SEARCH_PRODUCT_DOWN_DLQ,
                MqConstant.Exchanges.SEARCH_PRODUCT_DOWN_DLX,
                MqConstant.Queues.SEARCH_PRODUCT_DOWN_DLQ);
    }
}
