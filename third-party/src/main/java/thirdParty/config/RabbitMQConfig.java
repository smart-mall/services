package thirdParty.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * third-party 侧消费 {@code product.deleted} 所需的交换机与队列，用来清 MinIO 里的孤儿文件。
 *
 * <p>结构和 coupon 侧完全对称（业务队列 → DLX → TTL 重试队列 → 回到业务队列；重试到上限由
 * 监听器投进死信队列），只是换了前缀。<b>两个服务各自一个队列、各自消费同一条消息</b>，
 * 互不阻塞：coupon 挂了不影响清文件，反过来的道理也一样。</p>
 */
@Configuration
public class RabbitMQConfig {

    /** 和 product 侧的声明必须完全一致（同名同参数），否则 406 PRECONDITION_FAILED */
    public static final String PRODUCT_EVENT_EXCHANGE = "product-event-exchange";
    public static final String PRODUCT_DELETED_ROUTING_KEY = "product.deleted";

    public static final String PRODUCT_DELETED_QUEUE = "thirdparty.product.deleted.queue";
    public static final String PRODUCT_DELETED_DLX = "thirdparty.product.deleted.dlx";
    public static final String PRODUCT_DELETED_RETRY_QUEUE = "thirdparty.product.deleted.retry.queue";
    public static final String PRODUCT_DELETED_DLQ = "thirdparty.product.deleted.dlq";

    public static final String RETRY_ROUTING_KEY = "thirdparty.product.deleted.retry";
    public static final String DLQ_ROUTING_KEY = "thirdparty.product.deleted.dlq";

    private static final int RETRY_TTL_MILLIS = 60_000;

    @Bean
    public Exchange productEventExchange() {
        return new TopicExchange(PRODUCT_EVENT_EXCHANGE, true, false);
    }

    @Bean
    public Queue thirdPartyProductDeletedQueue() {
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("x-dead-letter-exchange", PRODUCT_DELETED_DLX);
        arguments.put("x-dead-letter-routing-key", RETRY_ROUTING_KEY);
        return new Queue(PRODUCT_DELETED_QUEUE, true, false, false, arguments);
    }

    @Bean
    public Binding thirdPartyProductDeletedBinding() {
        return new Binding(PRODUCT_DELETED_QUEUE,
                Binding.DestinationType.QUEUE,
                PRODUCT_EVENT_EXCHANGE,
                PRODUCT_DELETED_ROUTING_KEY,
                null);
    }

    @Bean
    public Exchange thirdPartyProductDeletedDlx() {
        return new DirectExchange(PRODUCT_DELETED_DLX, true, false);
    }

    /** 消息在这里躺 1 分钟，到期死信回业务交换机，等于延迟重投 */
    @Bean
    public Queue thirdPartyProductDeletedRetryQueue() {
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("x-dead-letter-exchange", PRODUCT_EVENT_EXCHANGE);
        arguments.put("x-dead-letter-routing-key", PRODUCT_DELETED_ROUTING_KEY);
        arguments.put("x-message-ttl", RETRY_TTL_MILLIS);
        return new Queue(PRODUCT_DELETED_RETRY_QUEUE, true, false, false, arguments);
    }

    @Bean
    public Binding thirdPartyProductDeletedRetryBinding() {
        return new Binding(PRODUCT_DELETED_RETRY_QUEUE,
                Binding.DestinationType.QUEUE,
                PRODUCT_DELETED_DLX,
                RETRY_ROUTING_KEY,
                null);
    }

    @Bean
    public Queue thirdPartyProductDeletedDlq() {
        return new Queue(PRODUCT_DELETED_DLQ, true, false, false);
    }

    @Bean
    public Binding thirdPartyProductDeletedDlqBinding() {
        return new Binding(PRODUCT_DELETED_DLQ,
                Binding.DestinationType.QUEUE,
                PRODUCT_DELETED_DLX,
                DLQ_ROUTING_KEY,
                null);
    }
}
