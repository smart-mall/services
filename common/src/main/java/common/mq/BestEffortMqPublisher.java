package common.mq;

import org.springframework.amqp.rabbit.core.RabbitTemplate;

/**
 * 尽力而为的投递实现：直接转给 {@link RabbitTemplate}，不重试、不落状态。
 *
 * <p>投递结果只出现在模板的 confirm / returns 回调日志里，broker 不可达或进程崩溃时消息会丢。
 */
public class BestEffortMqPublisher implements MqPublisher {

    private final RabbitTemplate rabbitTemplate;

    public BestEffortMqPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    /** {@inheritDoc} */
    @Override
    public void publish(String exchange, String routingKey, Object payload) {
        rabbitTemplate.convertAndSend(exchange, routingKey, payload);
    }
}
