package ware.config;

import common.mq.MqBuilder;
import common.mq.MqConstant;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * ware 侧的 MQ 拓扑。stock.release.stock.queue 由本服务（消费方）声明，
 * 并绑到 stock 与 order 两个交换机。
 *
 * <p>另外声明删商品所需的队列组（消费 {@code product.deleted} 后清掉零库存行），
 * 结构与 coupon / third-party 那两组一致：业务队列 + 自己的 DLX + TTL 重试队列 + 死信队列。</p>
 */
@Configuration
public class RabbitMQConfig {

    @Bean
    public Exchange stockEventExchange() {
        return MqBuilder.topicExchange(MqConstant.Exchanges.STOCK_EVENT);
    }

    /** 本服务要往它上面绑队列，所以也声明一次，避免 ware 先启动时绑定引用不存在的交换机 */
    @Bean
    public Exchange orderEventExchange() {
        return MqBuilder.topicExchange(MqConstant.Exchanges.ORDER_EVENT);
    }

    /** 同上：本服务要往它上面绑两个队列（业务 + 重试），所以也声明一次 */
    @Bean
    public Exchange productEventExchange() {
        return MqBuilder.topicExchange(MqConstant.Exchanges.PRODUCT_EVENT);
    }

    /** 库存释放队列：接收 StockLockedTo 与 OrderTo 两种消息体 */
    @Bean
    public Queue stockReleaseStockQueue() {
        return MqBuilder.durableQueue(MqConstant.Queues.STOCK_RELEASE);
    }

    /** 延迟队列：TTL 2 分钟后死信到 stock.release */
    @Bean
    public Queue stockDelay() {
        return MqBuilder.ttlQueue(
                MqConstant.Queues.STOCK_DELAY,
                MqConstant.Exchanges.STOCK_EVENT,
                MqConstant.RoutingKeys.STOCK_RELEASE,
                MqConstant.TtlMillis.STOCK_LOCK_RELEASE);
    }

    /** stock.release.# → 释放队列 */
    @Bean
    public Binding stockLocked() {
        return MqBuilder.bind(
                MqConstant.Queues.STOCK_RELEASE,
                MqConstant.Exchanges.STOCK_EVENT,
                MqConstant.RoutingKeys.STOCK_RELEASE_PATTERN);
    }

    /** stock.locked → 延迟队列 */
    @Bean
    public Binding stockLockedBinding() {
        return MqBuilder.bind(
                MqConstant.Queues.STOCK_DELAY,
                MqConstant.Exchanges.STOCK_EVENT,
                MqConstant.RoutingKeys.STOCK_LOCKED);
    }

    /** order.release.other.# → 释放队列 */
    @Bean
    public Binding stockReleaseFromOrderBinding() {
        return MqBuilder.bind(
                MqConstant.Queues.STOCK_RELEASE,
                MqConstant.Exchanges.ORDER_EVENT,
                MqConstant.RoutingKeys.ORDER_RELEASE_OTHER_PATTERN);
    }

    // ── 商品删除后清掉已删 sku 的零库存行 ──

    /** 业务队列。消费失败 nack 后由自己的 DLX 接走 */
    @Bean
    public Queue wareProductDeletedQueue() {
        return MqBuilder.deadLetterQueue(
                MqConstant.Queues.WARE_PRODUCT_DELETED,
                MqConstant.Exchanges.WARE_PRODUCT_DELETED_DLX,
                MqConstant.RoutingKeys.WARE_PRODUCT_DELETED_RETRY);
    }

    @Bean
    public Binding wareProductDeletedBinding() {
        return MqBuilder.bind(
                MqConstant.Queues.WARE_PRODUCT_DELETED,
                MqConstant.Exchanges.PRODUCT_EVENT,
                MqConstant.RoutingKeys.PRODUCT_DELETED);
    }

    @Bean
    public Exchange wareProductDeletedDlx() {
        return MqBuilder.directExchange(MqConstant.Exchanges.WARE_PRODUCT_DELETED_DLX);
    }

    /** 重试队列。躺 TTL 后死信回业务交换机，等于"延迟 1 分钟再投一次" */
    @Bean
    public Queue wareProductDeletedRetryQueue() {
        return MqBuilder.ttlQueue(
                MqConstant.Queues.WARE_PRODUCT_DELETED_RETRY,
                MqConstant.Exchanges.PRODUCT_EVENT,
                MqConstant.RoutingKeys.PRODUCT_DELETED,
                MqConstant.TtlMillis.PRODUCT_DELETED_RETRY);
    }

    @Bean
    public Binding wareProductDeletedRetryBinding() {
        return MqBuilder.bind(
                MqConstant.Queues.WARE_PRODUCT_DELETED_RETRY,
                MqConstant.Exchanges.WARE_PRODUCT_DELETED_DLX,
                MqConstant.RoutingKeys.WARE_PRODUCT_DELETED_RETRY);
    }

    /** 死信队列。重试到上限仍失败的消息放这里等人工看，没有任何消费者 */
    @Bean
    public Queue wareProductDeletedDlq() {
        return MqBuilder.durableQueue(MqConstant.Queues.WARE_PRODUCT_DELETED_DLQ);
    }

    @Bean
    public Binding wareProductDeletedDlqBinding() {
        return MqBuilder.bind(
                MqConstant.Queues.WARE_PRODUCT_DELETED_DLQ,
                MqConstant.Exchanges.WARE_PRODUCT_DELETED_DLX,
                MqConstant.Queues.WARE_PRODUCT_DELETED_DLQ);
    }
}
