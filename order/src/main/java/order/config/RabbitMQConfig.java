package order.config;

import common.mq.MqBuilder;
import common.mq.MqConstant;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * order 侧的 MQ 拓扑。stock.release.stock.queue 属于 ware，由 ware 声明。
 */
@Configuration
public class RabbitMQConfig {

    /** 延迟队列：TTL 1 分钟后死信到 order.release.order */
    @Bean
    public Queue orderDelayQueue() {
        return MqBuilder.ttlQueue(
                MqConstant.Queues.ORDER_DELAY,
                MqConstant.Exchanges.ORDER_EVENT,
                MqConstant.RoutingKeys.ORDER_RELEASE,
                MqConstant.TtlMillis.ORDER_CLOSE);
    }

    /** 关单队列 */
    @Bean
    public Queue orderReleaseQueue() {
        return MqBuilder.durableQueue(MqConstant.Queues.ORDER_RELEASE);
    }

    /** 秒杀建单队列 */
    @Bean
    public Queue orderSecKillOrrderQueue() {
        return MqBuilder.durableQueue(MqConstant.Queues.ORDER_SECKILL);
    }

    @Bean
    public Exchange orderEventExchange() {
        return MqBuilder.topicExchange(MqConstant.Exchanges.ORDER_EVENT);
    }

    /** order.create.order → 延迟队列 */
    @Bean
    public Binding orderCreateBinding() {
        return MqBuilder.bind(
                MqConstant.Queues.ORDER_DELAY,
                MqConstant.Exchanges.ORDER_EVENT,
                MqConstant.RoutingKeys.ORDER_CREATE);
    }

    /** order.release.order → 关单队列 */
    @Bean
    public Binding orderReleaseBinding() {
        return MqBuilder.bind(
                MqConstant.Queues.ORDER_RELEASE,
                MqConstant.Exchanges.ORDER_EVENT,
                MqConstant.RoutingKeys.ORDER_RELEASE);
    }

    /** order.seckill.order → 秒杀建单队列 */
    @Bean
    public Binding orderSecKillOrrderQueueBinding() {
        return MqBuilder.bind(
                MqConstant.Queues.ORDER_SECKILL,
                MqConstant.Exchanges.ORDER_EVENT,
                MqConstant.RoutingKeys.ORDER_SECKILL);
    }
}
