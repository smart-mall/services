package common.mq.outbox;

import common.mq.MqPublisher;

/**
 * 至少一次的投递：先落本地消息表，事务提交后再投，失败由重投任务兜底。
 */
public class ReliableMqPublisher implements MqPublisher {

    private final OutboxPublisher outboxPublisher;

    public ReliableMqPublisher(OutboxPublisher outboxPublisher) {
        this.outboxPublisher = outboxPublisher;
    }

    @Override
    public void publish(String exchange, String routingKey, Object payload) {
        outboxPublisher.publish(exchange, routingKey, payload);
    }
}
