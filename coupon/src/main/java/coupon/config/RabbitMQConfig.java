package coupon.config;

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
 * coupon 侧消费 {@code product.deleted} 所需的交换机与队列。
 *
 * <p>结构（失败路径靠"队列自己的 DLX + TTL 重试队列"实现）：</p>
 *
 * <pre>
 * product-event-exchange ──product.deleted──> coupon.product.deleted.queue
 *                                                    │ 消费失败 basicNack(requeue=false)
 *                                                    ▼ DLX
 *                                     coupon.product.deleted.dlx
 *                                                    │ coupon.product.deleted.retry
 *                                                    ▼
 *                              coupon.product.deleted.retry.queue  (TTL 1 分钟)
 *                                                    │ 到期死信回 product-event-exchange
 *                                                    └──> 回到 coupon.product.deleted.queue（下一轮）
 *
 * 重试到上限仍失败 → 由监听器直接投到 coupon.product.deleted.dlq（消费端手动处理）
 * </pre>
 *
 * <p><b>为什么不能只是 basicReject(requeue=true)：</b>那样会把消息塞回队头全速重投，
 * 没有退避、没有上限、没有出口 —— 一条毒消息能把消费者打死，而且完全静默。
 * 项目里现有三个监听器都是这个写法，新代码不照抄。</p>
 */
@Configuration
public class RabbitMQConfig {

    /** 和 product 侧的声明必须完全一致（同名同参数），否则 406 PRECONDITION_FAILED */
    public static final String PRODUCT_EVENT_EXCHANGE = "product-event-exchange";
    public static final String PRODUCT_DELETED_ROUTING_KEY = "product.deleted";

    public static final String PRODUCT_DELETED_QUEUE = "coupon.product.deleted.queue";
    public static final String PRODUCT_DELETED_DLX = "coupon.product.deleted.dlx";
    public static final String PRODUCT_DELETED_RETRY_QUEUE = "coupon.product.deleted.retry.queue";
    public static final String PRODUCT_DELETED_DLQ = "coupon.product.deleted.dlq";

    public static final String RETRY_ROUTING_KEY = "coupon.product.deleted.retry";
    public static final String DLQ_ROUTING_KEY = "coupon.product.deleted.dlq";

    /** 重试间隔。只做固定 1 分钟，够把"coupon 数据库短暂抖动/重启"这类失败熬过去 */
    private static final int RETRY_TTL_MILLIS = 60_000;

    @Bean
    public Exchange productEventExchange() {
        return new TopicExchange(PRODUCT_EVENT_EXCHANGE, true, false);
    }

    /** 业务队列。消费失败 nack 后由自己的 DLX 接走 */
    @Bean
    public Queue couponProductDeletedQueue() {
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("x-dead-letter-exchange", PRODUCT_DELETED_DLX);
        arguments.put("x-dead-letter-routing-key", RETRY_ROUTING_KEY);
        return new Queue(PRODUCT_DELETED_QUEUE, true, false, false, arguments);
    }

    @Bean
    public Binding couponProductDeletedBinding() {
        return new Binding(PRODUCT_DELETED_QUEUE,
                Binding.DestinationType.QUEUE,
                PRODUCT_EVENT_EXCHANGE,
                PRODUCT_DELETED_ROUTING_KEY,
                null);
    }

    @Bean
    public Exchange couponProductDeletedDlx() {
        return new DirectExchange(PRODUCT_DELETED_DLX, true, false);
    }

    /**
     * 重试队列。消息在这里躺 RETRY_TTL_MILLIS，到期后按 x-dead-letter-* 死信回业务交换机，
     * 于是又回到业务队列 —— 等于"延迟 1 分钟再投一次"。
     */
    @Bean
    public Queue couponProductDeletedRetryQueue() {
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("x-dead-letter-exchange", PRODUCT_EVENT_EXCHANGE);
        arguments.put("x-dead-letter-routing-key", PRODUCT_DELETED_ROUTING_KEY);
        arguments.put("x-message-ttl", RETRY_TTL_MILLIS);
        return new Queue(PRODUCT_DELETED_RETRY_QUEUE, true, false, false, arguments);
    }

    @Bean
    public Binding couponProductDeletedRetryBinding() {
        return new Binding(PRODUCT_DELETED_RETRY_QUEUE,
                Binding.DestinationType.QUEUE,
                PRODUCT_DELETED_DLX,
                RETRY_ROUTING_KEY,
                null);
    }

    /** 死信队列。重试到上限仍失败的消息放这里，等人工看 —— 没有任何消费者自动消费它 */
    @Bean
    public Queue couponProductDeletedDlq() {
        return new Queue(PRODUCT_DELETED_DLQ, true, false, false);
    }

    @Bean
    public Binding couponProductDeletedDlqBinding() {
        return new Binding(PRODUCT_DELETED_DLQ,
                Binding.DestinationType.QUEUE,
                PRODUCT_DELETED_DLX,
                DLQ_ROUTING_KEY,
                null);
    }
}
