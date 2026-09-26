package common.mq.outbox;

import common.mq.MqPublisher;

/**
 * 至少一次语义的投递门面：先落本地消息表，事务提交后再投，失败由重投任务兜底。
 *
 * <p>链路位置：业务代码 → 本类 → {@link OutboxPublisher} → {@code mq_message} 表 → RabbitMQ；
 * 与尽力而为的 {@link common.mq.BestEffortMqPublisher} 互为替代实现。
 *
 * <p>无状态、线程安全：只把调用转给投递端。
 */
public class ReliableMqPublisher implements MqPublisher {

    private final OutboxPublisher outboxPublisher;

    public ReliableMqPublisher(OutboxPublisher outboxPublisher) {
        this.outboxPublisher = outboxPublisher;
    }

    /**
     * {@inheritDoc}
     *
     * <p>本实现额外要求：在业务事务内调用，消息记录才能与业务数据同生共死；无事务时立即落库并投递。
     */
    @Override
    public void publish(String exchange, String routingKey, Object payload) {
        outboxPublisher.publish(exchange, routingKey, payload);
    }
}
