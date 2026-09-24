package common.mq.outbox;

import java.util.Date;
import java.util.List;

/**
 * 本地消息表的读写。
 */
public interface OutboxStore {

    /**
     * 在调用方的事务里插入一条待投递消息。
     *
     * @return 消息 id
     */
    String savePending(String toExchange, String routingKey, String classType, String content);

    OutboxMessage getById(String messageId);

    /** 待重投的消息：PENDING / ERROR，以及抢占后超时未完成的 CLAIMED */
    List<OutboxMessage> listForResend(int limit);

    /** CAS 抢占，返回 true 表示本实例抢到了这条 */
    boolean claim(String messageId);

    /** broker 已 ack */
    void markSent(String messageId);

    /** 投递失败，原因写入日志 */
    void markError(String messageId, String cause);

    int removeSentBefore(Date deadline);
}
