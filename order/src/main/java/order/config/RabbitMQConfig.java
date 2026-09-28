package order.config;

import common.mq.MqBuilder;
import common.mq.MqConstant;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * order 侧的 RabbitMQ 拓扑：声明订单事件交换机、延迟 / 超时关单 / 秒杀建单队列及其绑定。
 *
 * <p>不带配置前缀，由 order 服务启动时装配；队列名、交换机名与路由键统一取自 {@link MqConstant}，
 * 实体由 {@link MqBuilder} 构造。</p>
 *
 * <p>{@code ware.stock-release.queue} 属于 ware，由 ware 自行声明，本类不涉及。</p>
 */
@Configuration
public class RabbitMQConfig {

    /**
     * 创建订单延迟队列：消息在队列内 TTL 1 分钟后，以 {@code order.timed-out} 为路由键死信进超时关单队列。
     *
     * @return 带 TTL 与死信参数的持久化队列
     */
    @Bean
    public Queue orderCreatedDelayQueue() {
        return MqBuilder.ttlQueue(
                MqConstant.Queues.ORDER_CREATED_DELAY,
                MqConstant.Exchanges.ORDER,
                MqConstant.RoutingKeys.ORDER_TIMED_OUT,
                MqConstant.TtlMillis.ORDER_TIMEOUT);
    }

    /**
     * 创建超时关单队列，接收延迟队列死信出来的待关闭订单。
     *
     * @return 持久化队列
     */
    @Bean
    public Queue orderTimedOutQueue() {
        return MqBuilder.durableQueue(MqConstant.Queues.ORDER_TIMED_OUT);
    }

    /**
     * 创建秒杀建单队列，由 {@code OrderSeckillListener} 消费。
     *
     * @return 持久化队列
     */
    @Bean
    public Queue orderSeckillCreatedQueue() {
        return MqBuilder.durableQueue(MqConstant.Queues.ORDER_SECKILL_CREATED);
    }

    /**
     * 创建订单事件 topic 交换机，下单、超时关单、秒杀建单与订单关闭消息都路由到它。
     *
     * @return 持久化、非自动删除的 topic 交换机
     */
    @Bean
    public Exchange orderExchange() {
        return MqBuilder.topicExchange(MqConstant.Exchanges.ORDER);
    }

    /**
     * 把 {@code order.created} 绑定到订单延迟队列。
     *
     * @return 队列到交换机的绑定声明
     */
    @Bean
    public Binding orderCreatedBinding() {
        return MqBuilder.bind(
                MqConstant.Queues.ORDER_CREATED_DELAY,
                MqConstant.Exchanges.ORDER,
                MqConstant.RoutingKeys.ORDER_CREATED);
    }

    /**
     * 把 {@code order.timed-out} 绑定到超时关单队列。
     *
     * @return 队列到交换机的绑定声明
     */
    @Bean
    public Binding orderTimedOutBinding() {
        return MqBuilder.bind(
                MqConstant.Queues.ORDER_TIMED_OUT,
                MqConstant.Exchanges.ORDER,
                MqConstant.RoutingKeys.ORDER_TIMED_OUT);
    }

    /**
     * 把 {@code order.seckill-created} 绑定到秒杀建单队列。
     *
     * @return 队列到交换机的绑定声明
     */
    @Bean
    public Binding orderSeckillCreatedBinding() {
        return MqBuilder.bind(
                MqConstant.Queues.ORDER_SECKILL_CREATED,
                MqConstant.Exchanges.ORDER,
                MqConstant.RoutingKeys.ORDER_SECKILL_CREATED);
    }
}
