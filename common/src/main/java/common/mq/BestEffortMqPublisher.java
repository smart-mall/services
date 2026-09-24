package common.mq;

import org.springframework.amqp.rabbit.core.RabbitTemplate;

/**
 * 尽力而为实现：直接转给 RabbitTemplate，不重试、不记录状态。
 */
public class BestEffortMqPublisher implements MqPublisher {

    private final RabbitTemplate rabbitTemplate;

    public BestEffortMqPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void publish(String exchange, String routingKey, Object payload) {
        rabbitTemplate.convertAndSend(exchange, routingKey, payload);
    }
}
