package common.mq.outbox;

import java.util.Date;
import java.util.List;

/**
 * 本地消息表的读写接口。
 *
 * <p>实现方必须保证：{@link #savePending} 在调用方事务内插入，{@link #claim} 是数据库侧的 CAS 抢占；
 * 少了前者会出现"业务回滚了消息却投出去"，少了后者多实例会重复投递。
 */
public interface OutboxStore {

    /**
     * 在调用方的事务里插入一条待投递消息。
     *
     * @param toExchange 目标交换机名
     * @param routingKey 目标路由键
     * @param classType  消息体全限定类名，重投时用于反序列化
     * @param content    消息体 JSON
     * @return 新生成的消息 id，同时作为 CorrelationData id
     */
    String savePending(String toExchange, String routingKey, String classType, String content);

    /**
     * 按消息 id 查询记录。
     *
     * @param messageId 消息 id
     * @return 消息记录；不存在时返回 {@code null}
     */
    OutboxMessage getById(String messageId);

    /**
     * 返回待重投的消息：PENDING / ERROR，以及抢占后超时未完成的 CLAIMED。
     *
     * @param limit 单次返回的最大条数，必须为正数
     * @return 待重投消息，按创建时间升序
     */
    List<OutboxMessage> listForResend(int limit);

    /**
     * CAS 抢占，返回 true 表示本实例抢到了这条。
     *
     * @param messageId 消息 id
     * @return {@code true} 表示抢占成功、可以投递；已被其他实例抢占或已发送时返回 {@code false}
     */
    boolean claim(String messageId);

    /**
     * 把消息置为 broker 已 ack。
     *
     * @param messageId 消息 id
     */
    void markSent(String messageId);

    /**
     * 把消息置为投递失败，等待重投。
     *
     * @param messageId 消息 id
     * @param cause     失败原因，写入日志用于排查
     */
    void markError(String messageId, String cause);

    /**
     * 删除已发送且最后更新时间早于截止时刻的记录。
     *
     * @param deadline 截止时刻
     * @return 删除条数
     */
    int removeSentBefore(Date deadline);
}
