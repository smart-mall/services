package common.mq;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;

import java.util.HashMap;
import java.util.Map;

/**
 * 声明 AMQP 资源（交换机 / 队列 / 绑定）的统一入口。
 *
 * <p>各服务的 {@code RabbitMQConfig} 通过本类构造 bean，配合 {@link MqConstant} 的常量，
 * 做到"拓扑只有一处定义、声明只有一种写法"。之前每个配置类各自手写
 * {@code HashMap} 拼 {@code x-dead-letter-*} 参数，参数名写错编译器不报错，
 * 是"配置很乱"的一个主要来源。</p>
 *
 * <p><b>为什么用显式构造器 + 集中拼参数，而不是 {@code QueueBuilder}</b>：
 * {@code x-message-ttl} 的值类型（{@code Integer} 还是 {@code Long}）会影响 broker
 * 对"队列参数是否等价"的判断，不等价就是 406。本项目这些队列已经在 broker 上存在，
 * 而显式构造器能保证声明出来的参数与既有队列<b>逐项一致</b>。等这些改动经过一轮
 * 真实启动验证之后，可以再统一迁移到 {@code QueueBuilder}（更符合 Spring AMQP 习惯），
 * 那属于风格优化，不在这轮的正确性范围内。</p>
 *
 * @see MqConstant 拓扑名称的唯一目录
 */
public final class MqBuilder {

    private MqBuilder() {
    }

    /** durable 的 TopicExchange，无自动删除 */
    public static Exchange topicExchange(String name) {
        return new TopicExchange(name, true, false);
    }

    /** durable 的 DirectExchange，无自动删除（死信交换机用） */
    public static Exchange directExchange(String name) {
        return new DirectExchange(name, true, false);
    }

    /** durable 的普通队列，非排他、不自动删除 */
    public static Queue durableQueue(String name) {
        return new Queue(name, true, false, false);
    }

    /**
     * 带死信配置、<b>没有</b> TTL 的队列（业务队列：消费失败时把消息交给 DLX）。
     *
     * @param name                  队列名
     * @param deadLetterExchange    死信交换机
     * @param deadLetterRoutingKey  死信路由键
     */
    public static Queue deadLetterQueue(String name, String deadLetterExchange, String deadLetterRoutingKey) {
        return new Queue(name, true, false, false,
                deadLetterArguments(deadLetterExchange, deadLetterRoutingKey));
    }

    /**
     * 带死信配置与 TTL 的队列。
     *
     * <p>项目里两种用途共用这一个形状，都是"消息躺够时间后死信到别处"：</p>
     * <ul>
     *   <li><b>延迟队列</b>：业务方投进来，TTL 到期后死信回业务交换机，由别的队列消费
     *       （{@code order.delay.queue}、{@code stock.delay.queue}）；</li>
     *   <li><b>重试队列</b>：消费失败的消息被 DLX 转进来，TTL 到期后死信回业务交换机，
     *       等于"延迟一段时间再投一次"（{@code *.product.deleted.retry.queue}）。</li>
     * </ul>
     *
     * @param name                  队列名
     * @param deadLetterExchange    死信交换机
     * @param deadLetterRoutingKey  死信路由键
     * @param ttlMillis             消息存活时间（毫秒），会写成 {@code x-message-ttl}
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
     * <p>用显式构造器而不是 {@code BindingBuilder}：绑定声明只用到交换机的<b>名字</b>，
     * 而 {@code BindingBuilder.to(...)} 需要区分交换机类型（本项目的交换机既有 Topic 也有
     * Direct），用错重载不会编译报错却会写错语义。显式构造器不存在这个问题。</p>
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
