package ware.config;

import common.mq.MqBuilder;
import common.mq.MqConstant;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * ware 侧的 MQ 拓扑：库存释放所需的交换机、队列与绑定，以及删商品后清理零库存行的队列组。
 *
 * <p>库存释放队列由本服务（消费方）声明，同时绑到 stock 与 order 两个交换机上：
 * {@code stock.release.#} 收延迟 2 分钟到期的释放消息，{@code order.release.other.#} 收订单关闭的释放消息。</p>
 *
 * <p>删商品那组是业务队列 + 自己的 DLX + TTL 重试队列 + 死信队列，消费失败经重试队列延迟 1 分钟回投，
 * 重试到上限由监听器投进死信队列等人工处理。</p>
 */
@Configuration
public class RabbitMQConfig {

    /**
     * 库存事件交换机，topic 类型；库存锁定成功由本服务投到它上面。
     *
     * @return 持久化的 topic 交换机
     */
    @Bean
    public Exchange stockEventExchange() {
        return MqBuilder.topicExchange(MqConstant.Exchanges.STOCK_EVENT);
    }

    /**
     * 订单事件交换机，topic 类型。
     *
     * <p>本服务要往它上面绑队列，所以也声明一次，避免 ware 先启动时绑定引用不存在的交换机。</p>
     *
     * @return 持久化的 topic 交换机
     */
    @Bean
    public Exchange orderEventExchange() {
        return MqBuilder.topicExchange(MqConstant.Exchanges.ORDER_EVENT);
    }

    /**
     * 商品事件交换机，topic 类型。
     *
     * <p>本服务要往它上面绑两个队列（业务 + 重试），所以也声明一次。</p>
     *
     * @return 持久化的 topic 交换机
     */
    @Bean
    public Exchange productEventExchange() {
        return MqBuilder.topicExchange(MqConstant.Exchanges.PRODUCT_EVENT);
    }

    /**
     * 库存释放队列，接收 {@code StockLockedTo} 与 {@code OrderTo} 两种消息体。
     *
     * @return 持久化队列
     */
    @Bean
    public Queue stockReleaseStockQueue() {
        return MqBuilder.durableQueue(MqConstant.Queues.STOCK_RELEASE);
    }

    /**
     * 库存延迟队列，消息滞留 2 分钟后以 {@code stock.release} 死信进释放队列。
     *
     * @return 带 TTL 与死信参数的持久化队列
     */
    @Bean
    public Queue stockDelay() {
        return MqBuilder.ttlQueue(
                MqConstant.Queues.STOCK_DELAY,
                MqConstant.Exchanges.STOCK_EVENT,
                MqConstant.RoutingKeys.STOCK_RELEASE,
                MqConstant.TtlMillis.STOCK_LOCK_RELEASE);
    }

    /**
     * 把释放队列绑到库存事件交换机上，接收 {@code stock.release.#} 匹配的延迟到期消息。
     *
     * @return 队列与交换机的绑定
     */
    @Bean
    public Binding stockLocked() {
        return MqBuilder.bind(
                MqConstant.Queues.STOCK_RELEASE,
                MqConstant.Exchanges.STOCK_EVENT,
                MqConstant.RoutingKeys.STOCK_RELEASE_PATTERN);
    }

    /**
     * 把延迟队列绑到库存事件交换机上，只接收 {@code stock.locked} 的锁定成功消息。
     *
     * @return 队列与交换机的绑定
     */
    @Bean
    public Binding stockLockedBinding() {
        return MqBuilder.bind(
                MqConstant.Queues.STOCK_DELAY,
                MqConstant.Exchanges.STOCK_EVENT,
                MqConstant.RoutingKeys.STOCK_LOCKED);
    }

    /**
     * 把释放队列绑到订单事件交换机上，接收 {@code order.release.other.#} 匹配的订单关闭消息。
     *
     * @return 队列与交换机的绑定
     */
    @Bean
    public Binding stockReleaseFromOrderBinding() {
        return MqBuilder.bind(
                MqConstant.Queues.STOCK_RELEASE,
                MqConstant.Exchanges.ORDER_EVENT,
                MqConstant.RoutingKeys.ORDER_RELEASE_OTHER_PATTERN);
    }

    // ── 商品删除后清掉已删 sku 的零库存行 ──

    /**
     * 业务队列，消费失败 nack 后由自己的 DLX 接走。
     *
     * @return 绑定了死信交换机的业务队列
     */
    @Bean
    public Queue wareProductDeletedQueue() {
        return MqBuilder.deadLetterQueue(
                MqConstant.Queues.WARE_PRODUCT_DELETED,
                MqConstant.Exchanges.WARE_PRODUCT_DELETED_DLX,
                MqConstant.RoutingKeys.WARE_PRODUCT_DELETED_RETRY);
    }

    /**
     * 把业务队列绑到商品事件交换机上，只接收路由键 {@code product.deleted} 的消息。
     *
     * @return 队列与交换机的绑定
     */
    @Bean
    public Binding wareProductDeletedBinding() {
        return MqBuilder.bind(
                MqConstant.Queues.WARE_PRODUCT_DELETED,
                MqConstant.Exchanges.PRODUCT_EVENT,
                MqConstant.RoutingKeys.PRODUCT_DELETED);
    }

    /**
     * 业务队列的死信交换机，direct 类型。
     *
     * @return 持久化的 direct 交换机
     */
    @Bean
    public Exchange wareProductDeletedDlx() {
        return MqBuilder.directExchange(MqConstant.Exchanges.WARE_PRODUCT_DELETED_DLX);
    }

    /**
     * 重试队列，消息在此滞留 1 分钟后死信回商品事件交换机，等价于延迟重投。
     *
     * @return 带 TTL 与死信参数的持久化队列
     */
    @Bean
    public Queue wareProductDeletedRetryQueue() {
        return MqBuilder.ttlQueue(
                MqConstant.Queues.WARE_PRODUCT_DELETED_RETRY,
                MqConstant.Exchanges.PRODUCT_EVENT,
                MqConstant.RoutingKeys.PRODUCT_DELETED,
                MqConstant.TtlMillis.PRODUCT_DELETED_RETRY);
    }

    /**
     * 把重试队列绑到 DLX 上，路由键与重试队列同名。
     *
     * @return 重试队列与 DLX 的绑定
     */
    @Bean
    public Binding wareProductDeletedRetryBinding() {
        return MqBuilder.bind(
                MqConstant.Queues.WARE_PRODUCT_DELETED_RETRY,
                MqConstant.Exchanges.WARE_PRODUCT_DELETED_DLX,
                MqConstant.RoutingKeys.WARE_PRODUCT_DELETED_RETRY);
    }

    /**
     * 死信队列，重试到上限仍失败的消息放这里等人工看，没有任何消费者。
     *
     * @return 持久化死信队列
     */
    @Bean
    public Queue wareProductDeletedDlq() {
        return MqBuilder.durableQueue(MqConstant.Queues.WARE_PRODUCT_DELETED_DLQ);
    }

    /**
     * 把死信队列绑到 DLX 上，路由键与队列名同值。
     *
     * @return 死信队列与 DLX 的绑定
     */
    @Bean
    public Binding wareProductDeletedDlqBinding() {
        return MqBuilder.bind(
                MqConstant.Queues.WARE_PRODUCT_DELETED_DLQ,
                MqConstant.Exchanges.WARE_PRODUCT_DELETED_DLX,
                MqConstant.Queues.WARE_PRODUCT_DELETED_DLQ);
    }
}
