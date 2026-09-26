package common.mq;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;

import java.util.HashMap;
import java.util.Map;

/**
 * 声明交换机、队列、绑定的静态工厂。
 *
 * <p>无状态、线程安全：交换机与队列统一建成持久化、非自动删除，各服务的拓扑配置只传名字与参数。
 */
public final class MqBuilder {

    private MqBuilder() {
    }

    /**
     * 创建 topic 交换机，按路由键的通配符匹配队列。
     *
     * @param name 交换机名，取 {@link MqConstant.Exchanges}
     * @return 持久化、非自动删除的 topic 交换机
     */
    public static Exchange topicExchange(String name) {
        return new TopicExchange(name, true, false);
    }

    /**
     * 创建 direct 交换机，按路由键全等匹配队列。
     *
     * @param name 交换机名，取 {@link MqConstant.Exchanges}
     * @return 持久化、非自动删除的 direct 交换机
     */
    public static Exchange directExchange(String name) {
        return new DirectExchange(name, true, false);
    }

    /**
     * 创建不带 TTL 与死信参数的持久化队列。
     *
     * @param name 队列名，取 {@link MqConstant.Queues}
     * @return 持久化队列
     */
    public static Queue durableQueue(String name) {
        return new Queue(name, true, false, false);
    }

    /**
     * 创建业务队列：无 TTL，消费失败被 nack 后死信到指定交换机。
     *
     * @param name                 队列名，取 {@link MqConstant.Queues}
     * @param deadLetterExchange   死信交换机名，取 {@link MqConstant.Exchanges}
     * @param deadLetterRoutingKey 死信路由键，取 {@link MqConstant.RoutingKeys}
     * @return 带死信参数的持久化队列
     */
    public static Queue deadLetterQueue(String name, String deadLetterExchange, String deadLetterRoutingKey) {
        return new Queue(name, true, false, false,
                deadLetterArguments(deadLetterExchange, deadLetterRoutingKey));
    }

    /**
     * 创建延迟 / 重试队列：消息在队列里躺够 TTL 后死信到指定交换机。
     *
     * @param name                 队列名，取 {@link MqConstant.Queues}
     * @param deadLetterExchange   死信交换机名，取 {@link MqConstant.Exchanges}
     * @param deadLetterRoutingKey 死信路由键，取 {@link MqConstant.RoutingKeys}
     * @param ttlMillis            消息存活毫秒数，取 {@link MqConstant.TtlMillis}
     * @return 带 TTL 与死信参数的持久化队列
     */
    public static Queue ttlQueue(String name, String deadLetterExchange, String deadLetterRoutingKey,
                                 int ttlMillis) {
        Map<String, Object> arguments = deadLetterArguments(deadLetterExchange, deadLetterRoutingKey);
        arguments.put("x-message-ttl", ttlMillis);
        return new Queue(name, true, false, false, arguments);
    }

    /**
     * 把队列绑到交换机上。
     *
     * @param queue      队列名，取 {@link MqConstant.Queues}
     * @param exchange   交换机名，取 {@link MqConstant.Exchanges}
     * @param routingKey 路由键；topic 交换机上可以带通配符，direct 交换机上必须全等匹配
     * @return 队列到交换机的绑定声明
     */
    public static Binding bind(String queue, String exchange, String routingKey) {
        return new Binding(queue, Binding.DestinationType.QUEUE, exchange, routingKey, null);
    }

    private static Map<String, Object> deadLetterArguments(String deadLetterExchange,
                                                           String deadLetterRoutingKey) {
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("x-dead-letter-exchange", deadLetterExchange);
        arguments.put("x-dead-letter-routing-key", deadLetterRoutingKey);
        return arguments;
    }
}
