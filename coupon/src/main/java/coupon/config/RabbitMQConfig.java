package coupon.config;

import common.mq.MqBuilder;
import common.mq.MqConstant;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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

    @Bean
    public Exchange productEventExchange() {
        return MqBuilder.topicExchange(MqConstant.Exchanges.PRODUCT_EVENT);
    }

    /** 业务队列。消费失败 nack 后由自己的 DLX 接走 */
    @Bean
    public Queue couponProductDeletedQueue() {
        return MqBuilder.deadLetterQueue(
                MqConstant.Queues.COUPON_PRODUCT_DELETED,
                MqConstant.Exchanges.COUPON_PRODUCT_DELETED_DLX,
                MqConstant.RoutingKeys.COUPON_PRODUCT_DELETED_RETRY);
    }

    @Bean
    public Binding couponProductDeletedBinding() {
        return MqBuilder.bind(
                MqConstant.Queues.COUPON_PRODUCT_DELETED,
                MqConstant.Exchanges.PRODUCT_EVENT,
                MqConstant.RoutingKeys.PRODUCT_DELETED);
    }

    @Bean
    public Exchange couponProductDeletedDlx() {
        return MqBuilder.directExchange(MqConstant.Exchanges.COUPON_PRODUCT_DELETED_DLX);
    }

    /**
     * 重试队列。消息在这里躺 TTL，到期后按 x-dead-letter-* 死信回业务交换机，
     * 于是又回到业务队列 —— 等于"延迟 1 分钟再投一次"。
     */
    @Bean
    public Queue couponProductDeletedRetryQueue() {
        return MqBuilder.ttlQueue(
                MqConstant.Queues.COUPON_PRODUCT_DELETED_RETRY,
                MqConstant.Exchanges.PRODUCT_EVENT,
                MqConstant.RoutingKeys.PRODUCT_DELETED,
                MqConstant.TtlMillis.PRODUCT_DELETED_RETRY);
    }

    @Bean
    public Binding couponProductDeletedRetryBinding() {
        return MqBuilder.bind(
                MqConstant.Queues.COUPON_PRODUCT_DELETED_RETRY,
                MqConstant.Exchanges.COUPON_PRODUCT_DELETED_DLX,
                MqConstant.RoutingKeys.COUPON_PRODUCT_DELETED_RETRY);
    }

    /** 死信队列。重试到上限仍失败的消息放这里，等人工看 —— 没有任何消费者自动消费它 */
    @Bean
    public Queue couponProductDeletedDlq() {
        return MqBuilder.durableQueue(MqConstant.Queues.COUPON_PRODUCT_DELETED_DLQ);
    }

    /** 死信队列的绑定，路由键与队列名同值 */
    @Bean
    public Binding couponProductDeletedDlqBinding() {
        return MqBuilder.bind(
                MqConstant.Queues.COUPON_PRODUCT_DELETED_DLQ,
                MqConstant.Exchanges.COUPON_PRODUCT_DELETED_DLX,
                MqConstant.Queues.COUPON_PRODUCT_DELETED_DLQ);
    }
}
