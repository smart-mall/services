package common.mq.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

/**
 * 本地消息表的投递端。投递状态由模板上的 confirm / returns 回调决定（回调在
 * {@code OutboxAutoConfiguration} 里设置），不由 convertAndSend 的返回决定。
 */
@Slf4j
public class OutboxPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final OutboxStore outboxStore;
    private final ObjectMapper objectMapper;

    public OutboxPublisher(RabbitTemplate rabbitTemplate, OutboxStore outboxStore,
                           ObjectMapper objectMapper) {
        this.rabbitTemplate = rabbitTemplate;
        this.outboxStore = outboxStore;
        this.objectMapper = objectMapper;
    }

    /**
     * 落库 + 投递。落库发生在调用方的事务里，投递等事务提交之后。
     */
    public void publish(String toExchange, String routingKey, Object payload) {
        String classType = payload.getClass().getName();
        String content;
        try {
            content = objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("消息体序列化失败: " + classType, e);
        }

        String messageId = outboxStore.savePending(toExchange, routingKey, classType, content);

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    send(messageId);
                }
            });
        } else {
            send(messageId);
        }
    }

    /** 按 id 投一条。投递失败不改状态，留给重投任务 */
    public void send(String messageId) {
        OutboxMessage message = outboxStore.getById(messageId);
        if (message == null) {
            log.warn("消息不存在，可能已被清理，messageId={}", messageId);
            return;
        }

        try {
            Object payload = objectMapper.readValue(message.getContent(), Class.forName(message.getClassType()));
            rabbitTemplate.convertAndSend(message.getToExchange(), message.getRoutingKey(), payload,
                    amqpMessage -> {
                        amqpMessage.getMessageProperties().setCorrelationId(messageId);
                        return amqpMessage;
                    },
                    new CorrelationData(messageId));
        } catch (ClassNotFoundException e) {
            // 类都找不到，重投一万次也发不出去，标成错误等人工处理
            outboxStore.markError(messageId, "消息体类型不存在: " + message.getClassType());
        } catch (Exception e) {
            log.error("投递消息失败，等待重投，messageId={}", messageId, e);
        }
    }

    /** 扫一轮待投递 / 错误消息重投，返回本轮处理条数 */
    public int resendPending(int limit) {
        List<OutboxMessage> pending = outboxStore.listForResend(limit);
        int count = 0;
        for (OutboxMessage message : pending) {
            if (outboxStore.claim(message.getMessageId())) {
                send(message.getMessageId());
                count++;
            }
        }
        return count;
    }
}
