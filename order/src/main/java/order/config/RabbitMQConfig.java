package order.config;

import common.mq.MqBuilder;
import common.mq.MqConstant;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * order 侧的 MQ 拓扑。
 *
 * <p><b>声明归属</b>：order 只声明自己的交换机与队列。
 * {@code stock.release.stock.queue} 属于 ware，已按"队列由消费方声明"的规则
 * 移交给 ware（连同它到本服务交换机的绑定），这里不再重复声明 ——
 * 同一个物理队列在两处声明，只能靠"参数必须一致否则 406"的注释维持，是隐患。</p>
 *
 * <p>拓扑形状：</p>
 * <pre>
 * order-event-exchange
 *   ├── order.create.order    ──> order.delay.queue（TTL 1 分钟）
 *   │                                 └── 死信 ──> order.release.order ──> order.release.order.queue
 *   ├── order.release.order   ──> order.release.order.queue
 *   ├── order.release.other.# ──> stock.release.stock.queue（该绑定已移到 ware）
 *   └── order.seckill.order   ──> order.seckill.order.queue
 * </pre>
 */
@Configuration
public class RabbitMQConfig {

    /**
     * 延迟队列：下单后 TTL 到期，死信回本交换机并按 {@code order.release.order} 进关单队列。
     *
     * <p>用"TTL + 死信"实现延迟，不需要额外的定时任务轮询。</p>
     */
    @Bean
    public Queue orderDelayQueue() {
        return MqBuilder.ttlQueue(
                MqConstant.Queues.ORDER_DELAY,
                MqConstant.Exchanges.ORDER_EVENT,
                MqConstant.RoutingKeys.ORDER_RELEASE,
                MqConstant.TtlMillis.ORDER_CLOSE);
    }

    /** 关单队列（普通队列） */
    @Bean
    public Queue orderReleaseQueue() {
        return MqBuilder.durableQueue(MqConstant.Queues.ORDER_RELEASE);
    }

    /** 秒杀建单队列（普通队列） */
    @Bean
    public Queue orderSecKillOrrderQueue() {
        return MqBuilder.durableQueue(MqConstant.Queues.ORDER_SECKILL);
    }

    @Bean
    public Exchange orderEventExchange() {
        return MqBuilder.topicExchange(MqConstant.Exchanges.ORDER_EVENT);
    }

    /** 下单成功 → 延迟队列 */
    @Bean
    public Binding orderCreateBinding() {
        return MqBuilder.bind(
                MqConstant.Queues.ORDER_DELAY,
                MqConstant.Exchanges.ORDER_EVENT,
                MqConstant.RoutingKeys.ORDER_CREATE);
    }

    /** 延迟到期后的关单事件 → 关单队列 */
    @Bean
    public Binding orderReleaseBinding() {
        return MqBuilder.bind(
                MqConstant.Queues.ORDER_RELEASE,
                MqConstant.Exchanges.ORDER_EVENT,
                MqConstant.RoutingKeys.ORDER_RELEASE);
    }

    /** 秒杀抢购成功 → 秒杀建单队列 */
    @Bean
    public Binding orderSecKillOrrderQueueBinding() {
        return MqBuilder.bind(
                MqConstant.Queues.ORDER_SECKILL,
                MqConstant.Exchanges.ORDER_EVENT,
                MqConstant.RoutingKeys.ORDER_SECKILL);
    }
}
