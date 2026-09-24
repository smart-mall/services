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
}
