package order.config;

import common.mq.MqBuilder;
import common.mq.MqConstant;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * order 侧的 RabbitMQ 拓扑：声明订单事件交换机、延迟 / 关单 / 秒杀建单队列及其绑定。
 *
 * <p>不带配置前缀，由 order 服务启动时装配；队列与交换机名统一取自 {@link MqConstant}，实体由 {@link MqBuilder} 构造。
 *
 * <p>{@code stock.release.stock.queue} 属于 ware，由 ware 自行声明，本类不涉及。
 */
@Configuration
public class RabbitMQConfig {

    /**
     * 创建订单延迟队列：消息在队列内 TTL 1 分钟后，以 {@code order.release.order} 为路由键死信进关单队列。
     *
     * @return 带 TTL 与死信参数的持久化队列
     */
    @Bean
    public Queue orderDelayQueue() {
        return MqBuilder.ttlQueue(
                MqConstant.Queues.ORDER_DELAY,
                MqConstant.Exchanges.ORDER_EVENT,
                MqConstant.RoutingKeys.ORDER_RELEASE,
                MqConstant.TtlMillis.ORDER_CLOSE);
    }

    /**
     * 创建关单队列，接收延迟队列死信出来的待关闭订单。
     *
     * @return 持久化队列
     */
    @Bean
    public Queue orderReleaseQueue() {
        return MqBuilder.durableQueue(MqConstant.Queues.ORDER_RELEASE);
    }

    /**
     * 创建秒杀建单队列，由 {@code OrderSeckillListener} 消费。
     *
     * @return 持久化队列
     */
    @Bean
    public Queue orderSecKillOrrderQueue() {
        return MqBuilder.durableQueue(MqConstant.Queues.ORDER_SECKILL);
    }

    /**
     * 创建订单事件 topic 交换机，订单创建、关单与秒杀建单消息都路由到它。
     *
     * @return 持久化、非自动删除的 topic 交换机
     */
    @Bean
    public Exchange orderEventExchange() {
        return MqBuilder.topicExchange(MqConstant.Exchanges.ORDER_EVENT);
    }

    /**
     * 把 {@code order.create.order} 绑定到订单延迟队列。
     *
     * @return 队列到交换机的绑定声明
     */
    @Bean
    public Binding orderCreateBinding() {
        return MqBuilder.bind(
                MqConstant.Queues.ORDER_DELAY,
                MqConstant.Exchanges.ORDER_EVENT,
                MqConstant.RoutingKeys.ORDER_CREATE);
    }

    /**
     * 把 {@code order.release.order} 绑定到关单队列。
     *
     * @return 队列到交换机的绑定声明
     */
    @Bean
    public Binding orderReleaseBinding() {
        return MqBuilder.bind(
                MqConstant.Queues.ORDER_RELEASE,
                MqConstant.Exchanges.ORDER_EVENT,
                MqConstant.RoutingKeys.ORDER_RELEASE);
    }

    /**
     * 把 {@code order.seckill.order} 绑定到秒杀建单队列。
     *
     * @return 队列到交换机的绑定声明
     */
    @Bean
    public Binding orderSecKillOrrderQueueBinding() {
        return MqBuilder.bind(
                MqConstant.Queues.ORDER_SECKILL,
                MqConstant.Exchanges.ORDER_EVENT,
                MqConstant.RoutingKeys.ORDER_SECKILL);
    }
}
