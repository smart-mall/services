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
 * <p>拓扑：业务队列消费失败 nack 后进 coupon.dlx，由它转进重试队列躺 1 分钟，到期死信回
 * product.exchange 并被重新路由到业务队列；重试到上限仍失败的消息由监听器直接投进死信队列。</p>
 *
 * <p><b>失败必须 basicNack(requeue=false)：</b>requeue=true 会把消息塞回队头全速重投，没有退避、
 * 没有上限、没有出口，一条毒消息能把消费者打死且完全静默；走 DLX 才能让消息先躺 TTL 再重投，
 * 并在超过上限后落进死信队列。</p>
 */
@Configuration
public class RabbitMQConfig {

    /**
     * 声明商品事件的 topic 交换机，product 投递删除事件与重试消息回流都走它。
     *
     * @return 持久化的 topic 交换机
     */
    @Bean
    public Exchange productExchange() {
        return MqBuilder.topicExchange(MqConstant.Exchanges.PRODUCT);
    }

    /**
     * 声明业务队列，消费失败被 nack 后由自己的 DLX 接走。
     *
     * @return 带死信参数的持久化队列
     */
    @Bean
    public Queue couponProductDeletedQueue() {
        return MqBuilder.deadLetterQueue(
                MqConstant.Queues.COUPON_PRODUCT_DELETED,
                MqConstant.Exchanges.COUPON_DLX,
                MqConstant.RoutingKeys.COUPON_PRODUCT_DELETED_RETRY);
    }

    /**
     * 把业务队列按 {@code product.deleted} 路由键绑到商品事件交换机上。
     *
     * @return 队列到交换机的绑定声明
     */
    @Bean
    public Binding couponProductDeletedBinding() {
        return MqBuilder.bind(
                MqConstant.Queues.COUPON_PRODUCT_DELETED,
                MqConstant.Exchanges.PRODUCT,
                MqConstant.RoutingKeys.PRODUCT_DELETED);
    }

    /**
     * 声明死信交换机，接收业务队列 nack 出来的消息。
     *
     * @return 持久化的 direct 交换机
     */
    @Bean
    public Exchange couponDlxExchange() {
        return MqBuilder.directExchange(MqConstant.Exchanges.COUPON_DLX);
    }

    /**
     * 声明重试队列：消息在这里躺够 TTL，到期后按 {@code x-dead-letter-*} 死信回业务交换机，
     * 于是又回到业务队列 —— 等于"延迟 1 分钟再投一次"。
     *
     * @return 带 TTL 与死信参数的持久化队列
     */
    @Bean
    public Queue couponProductDeletedRetryQueue() {
        return MqBuilder.ttlQueue(
                MqConstant.Queues.COUPON_PRODUCT_DELETED_RETRY,
                MqConstant.Exchanges.PRODUCT,
                MqConstant.RoutingKeys.PRODUCT_DELETED,
                MqConstant.TtlMillis.PRODUCT_DELETED_RETRY);
    }

    /**
     * 把重试队列绑到死信交换机上。
     *
     * @return 队列到交换机的绑定声明
     */
    @Bean
    public Binding couponProductDeletedRetryBinding() {
        return MqBuilder.bind(
                MqConstant.Queues.COUPON_PRODUCT_DELETED_RETRY,
                MqConstant.Exchanges.COUPON_DLX,
                MqConstant.RoutingKeys.COUPON_PRODUCT_DELETED_RETRY);
    }

    /**
     * 声明死信队列：重试到上限仍失败的消息放这里等人工看，没有任何消费者自动消费它。
     *
     * @return 不带 TTL 与死信参数的持久化队列
     */
    @Bean
    public Queue couponProductDeletedDlq() {
        return MqBuilder.durableQueue(MqConstant.Queues.COUPON_PRODUCT_DELETED_DLQ);
    }

    /**
     * 把死信队列绑到死信交换机上，路由键与队列名同值。
     *
     * @return 队列到交换机的绑定声明
     */
    @Bean
    public Binding couponProductDeletedDlqBinding() {
        return MqBuilder.bind(
                MqConstant.Queues.COUPON_PRODUCT_DELETED_DLQ,
                MqConstant.Exchanges.COUPON_DLX,
                MqConstant.RoutingKeys.COUPON_PRODUCT_DELETED_DLQ);
    }
}
