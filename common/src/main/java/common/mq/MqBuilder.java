package common.mq;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;

import java.util.HashMap;
import java.util.Map;

/**
 * 声明交换机、队列、绑定的统一入口。
 */
public final class MqBuilder {

    private MqBuilder() {
    }

    public static Exchange topicExchange(String name) {
        return new TopicExchange(name, true, false);
    }

    public static Exchange directExchange(String name) {
        return new DirectExchange(name, true, false);
    }

    public static Queue durableQueue(String name) {
        return new Queue(name, true, false, false);
    }

    /** 业务队列：无 TTL，消费失败时死信到 DLX */
    public static Queue deadLetterQueue(String name, String deadLetterExchange, String deadLetterRoutingKey) {
        return new Queue(name, true, false, false,
                deadLetterArguments(deadLetterExchange, deadLetterRoutingKey));
    }

    /** 延迟队列 / 重试队列：TTL 到期后死信到指定交换机 */
    public static Queue ttlQueue(String name, String deadLetterExchange, String deadLetterRoutingKey,
                                 int ttlMillis) {
        Map<String, Object> arguments = deadLetterArguments(deadLetterExchange, deadLetterRoutingKey);
        arguments.put("x-message-ttl", ttlMillis);
        return new Queue(name, true, false, false, arguments);
    }

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
