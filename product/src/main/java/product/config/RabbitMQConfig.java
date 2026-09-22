package product.config;

import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * product 自己的交换机。
 *
 * <p><b>队列和 binding 都由消费方自己声明</b>（coupon / third-party 各自 declare 自己的队列并绑到
 * 这个交换机上），product 只声明交换机。这样 product 不需要知道有哪些消费者，以后加一个消费者
 * 不用改 product。</p>
 *
 * <p>代价要说清楚：如果某个消费者从没启动过，它的队列和 binding 就不存在，消息会<b>不可路由</b>。
 * 这种情况由 {@code publisher-returns} 兜住 —— 消息被 return 回来，本地消息表标成「错误抵达」，
 * 定时任务持续重投，等消费方起来后自然就投进去了。所以这个代价是可接受的。</p>
 *
 * <p>另外：这个交换机在消费方那边也会声明一次（同名同参数）。AMQP 声明是幂等的，谁先起来谁建，
 * 但<b>参数必须完全一致</b>，否则会 406 PRECONDITION_FAILED。</p>
 */
@Configuration
public class RabbitMQConfig {

    public static final String PRODUCT_EVENT_EXCHANGE = "product-event-exchange";

    /** 商品被删除。消息体是 {@code common.to.mq.ProductDeletedTo} */
    public static final String PRODUCT_DELETED_ROUTING_KEY = "product.deleted";

    @Bean
    public Exchange productEventExchange() {
        //String name, boolean durable, boolean autoDelete, Map<String, Object> arguments
        return new TopicExchange(PRODUCT_EVENT_EXCHANGE, true, false);
    }
}
