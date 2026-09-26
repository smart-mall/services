package search.config;

import common.mq.MqBuilder;
import common.mq.MqConstant;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * search 侧消费 {@code product.down} 所需的交换机、队列与绑定。
 *
 * <p>拓扑：业务队列消费失败 → DLX → 重试队列躺 TTL → 重回业务队列；重试到上限由监听器投进 DLQ。
 */
@Configuration
public class RabbitMQConfig {

    /**
     * 商品事件主题交换机，{@code product.down} 由它路由到本模块的业务队列。
     *
     * @return 商品事件主题交换机
     */
    @Bean
    public Exchange productEventExchange() {
        return MqBuilder.topicExchange(MqConstant.Exchanges.PRODUCT_EVENT);
    }

    /**
     * 下架事件业务队列，消费失败的消息按死信参数进 DLX。
     *
     * @return 下架事件业务队列
     */
    @Bean
    public Queue searchProductDownQueue() {
        return MqBuilder.deadLetterQueue(
                MqConstant.Queues.SEARCH_PRODUCT_DOWN,
                MqConstant.Exchanges.SEARCH_PRODUCT_DOWN_DLX,
                MqConstant.RoutingKeys.SEARCH_PRODUCT_DOWN_RETRY);
    }

    /**
     * 把下架事件业务队列绑定到商品事件交换机。
     *
     * @return 业务队列与商品事件交换机的绑定
     */
    @Bean
    public Binding searchProductDownBinding() {
        return MqBuilder.bind(
                MqConstant.Queues.SEARCH_PRODUCT_DOWN,
                MqConstant.Exchanges.PRODUCT_EVENT,
                MqConstant.RoutingKeys.PRODUCT_DOWN);
    }

    /**
     * 下架事件的死信交换机，承接业务队列失败的消息。
     *
     * @return 下架事件死信交换机
     */
    @Bean
    public Exchange searchProductDownDlx() {
        return MqBuilder.directExchange(MqConstant.Exchanges.SEARCH_PRODUCT_DOWN_DLX);
    }

    /**
     * 下架事件重试队列，消息躺够 TTL 后按死信参数重回业务队列。
     *
     * @return 下架事件重试队列
     */
    @Bean
    public Queue searchProductDownRetryQueue() {
        return MqBuilder.ttlQueue(
                MqConstant.Queues.SEARCH_PRODUCT_DOWN_RETRY,
                MqConstant.Exchanges.PRODUCT_EVENT,
                MqConstant.RoutingKeys.PRODUCT_DOWN,
                MqConstant.TtlMillis.PRODUCT_DOWN_RETRY);
    }

    /**
     * 把重试队列绑定到死信交换机。
     *
     * @return 重试队列与死信交换机的绑定
     */
    @Bean
    public Binding searchProductDownRetryBinding() {
        return MqBuilder.bind(
                MqConstant.Queues.SEARCH_PRODUCT_DOWN_RETRY,
                MqConstant.Exchanges.SEARCH_PRODUCT_DOWN_DLX,
                MqConstant.RoutingKeys.SEARCH_PRODUCT_DOWN_RETRY);
    }

    /**
     * 下架事件死信队列，重试到上限的消息落到这里。
     *
     * @return 下架事件死信队列
     */
    @Bean
    public Queue searchProductDownDlq() {
        return MqBuilder.durableQueue(MqConstant.Queues.SEARCH_PRODUCT_DOWN_DLQ);
    }

    /**
     * 把死信队列绑定到死信交换机。
     *
     * <p>路由键用的是队列名，必须与监听器投递死信时使用的路由键一致，否则消息匹配不到这个绑定。
     *
     * @return 死信队列与死信交换机的绑定
     */
    @Bean
    public Binding searchProductDownDlqBinding() {
        return MqBuilder.bind(
                MqConstant.Queues.SEARCH_PRODUCT_DOWN_DLQ,
                MqConstant.Exchanges.SEARCH_PRODUCT_DOWN_DLX,
                MqConstant.Queues.SEARCH_PRODUCT_DOWN_DLQ);
    }
}
