package ware.config;

import common.mq.MqBuilder;
import common.mq.MqConstant;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * ware 侧的 MQ 拓扑。
 *
 * <p><b>声明归属</b>：{@code stock.release.stock.queue} 由本服务（它的消费方）声明，
 * 并在这里声明它到<b>两个</b>交换机的绑定：</p>
 * <ul>
 *   <li>本服务的 {@code stock-event-exchange}：锁库存成功 → 延迟 2 分钟 → 回查订单 → 解锁；</li>
 *   <li>order 的 {@code order-event-exchange}：订单关闭 → 立即解锁。</li>
 * </ul>
 *
 * <p>第二个绑定原本声明在 order 侧（order 需要知道 ware 的队列名），
 * 本次按"队列由消费方声明"的规则移到 ware。AMQP 声明是幂等的，
 * broker 上已存在的绑定不受影响，行为不变。</p>
 *
 * <p>拓扑形状：</p>
 * <pre>
 * stock-event-exchange
 *   ├── stock.locked      ──> stock.delay.queue（TTL 2 分钟）
 *   │                             └── 死信 ──> stock.release ──> stock.release.stock.queue
 *   └── stock.release.#   ──> stock.release.stock.queue
 *
 * order-event-exchange
 *   └── order.release.other.# ──> stock.release.stock.queue
 * </pre>
 */
@Configuration
public class RabbitMQConfig {

    /** 库存服务的事件交换机 */
    @Bean
    public Exchange stockEventExchange() {
        return MqBuilder.topicExchange(MqConstant.Exchanges.STOCK_EVENT);
    }

    /**
     * order 的事件交换机。
     *
     * <p>本服务要往它上面绑 {@code stock.release.stock.queue}，所以在这里也声明一次。
     * 这和 {@code product-event-exchange} 在消费方（coupon / third-party）也声明是同一个道理：
     * AMQP 声明是幂等的，谁先启动谁创建；不声明的话，ware 先于 order 启动时，
     * 绑定会引用一个尚不存在的交换机，以 404 NOT_FOUND 声明失败。</p>
     *
     * <p>同名同参数由 {@link MqConstant} 保证，不会 406。</p>
     */
    @Bean
    public Exchange orderEventExchange() {
        return MqBuilder.topicExchange(MqConstant.Exchanges.ORDER_EVENT);
    }

    /**
     * 库存释放队列。
     *
     * <p>它同时接收两种消息体：{@code StockLockedTo}（延迟回查后解锁）与
     * {@code OrderTo}（订单关闭后解锁），所以消费方 {@code StockReleaseListener}
     * 有两个 {@code @RabbitHandler}。</p>
     */
    @Bean
    public Queue stockReleaseStockQueue() {
        return MqBuilder.durableQueue(MqConstant.Queues.STOCK_RELEASE);
    }

    /**
     * 延迟队列：锁库存后 TTL 到期，死信回本交换机并按 {@code stock.release} 进释放队列。
     *
     * <p>它是"库存锁了但订单最终没成立"这个场景的兜底：订单事务可能回滚，
     * 而 ware 的事务已经独立提交，两者不在同一个事务里，所以只能靠延迟回查订单状态来补。</p>
     */
    @Bean
    public Queue stockDelay() {
        return MqBuilder.ttlQueue(
                MqConstant.Queues.STOCK_DELAY,
                MqConstant.Exchanges.STOCK_EVENT,
                MqConstant.RoutingKeys.STOCK_RELEASE,
                MqConstant.TtlMillis.STOCK_LOCK_RELEASE);
    }

    /** {@code stock.release.#} → 释放队列（含延迟到期后死信过来的 {@code stock.release}） */
    @Bean
    public Binding stockLocked() {
        return MqBuilder.bind(
                MqConstant.Queues.STOCK_RELEASE,
                MqConstant.Exchanges.STOCK_EVENT,
                MqConstant.RoutingKeys.STOCK_RELEASE_PATTERN);
    }

    /** {@code stock.locked} → 延迟队列 */
    @Bean
    public Binding stockLockedBinding() {
        return MqBuilder.bind(
                MqConstant.Queues.STOCK_DELAY,
                MqConstant.Exchanges.STOCK_EVENT,
                MqConstant.RoutingKeys.STOCK_LOCKED);
    }

    /** {@code order.release.other.#} → 释放队列（订单关闭，直接解锁库存） */
    @Bean
    public Binding stockReleaseFromOrderBinding() {
        return MqBuilder.bind(
                MqConstant.Queues.STOCK_RELEASE,
                MqConstant.Exchanges.ORDER_EVENT,
                MqConstant.RoutingKeys.ORDER_RELEASE_OTHER_PATTERN);
    }
}
