package common.mq.outbox;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * 本地消息表读写的 MyBatis-Plus 实现，条件与更新全部用 Wrapper 拼装。
 *
 * <p>无状态、线程安全：只持有 Mapper，抢占与状态流转都靠带条件的 UPDATE 在数据库侧完成。
 */
@Slf4j
public class OutboxStoreImpl implements OutboxStore {

    /** 刚提交的消息正由 afterCommit 投递，扫描时跳过 */
    private static final long RESEND_DELAY_MILLIS = 60_000L;

    /** CLAIMED 超过这个时长仍未完成，视为抢占方已挂，允许重新抢占 */
    private static final long CLAIM_TIMEOUT_MILLIS = 300_000L;

    private final OutboxDao outboxDao;

    public OutboxStoreImpl(OutboxDao outboxDao) {
        this.outboxDao = outboxDao;
    }

    /** {@inheritDoc} */
    @Override
    public String savePending(String toExchange, String routingKey, String classType, String content) {
        OutboxMessage message = new OutboxMessage();
        message.setMessageId(UUID.randomUUID().toString().replace("-", ""));
        message.setToExchange(toExchange);
        message.setRoutingKey(routingKey);
        message.setClassType(classType);
        message.setContent(content);
        message.setMessageStatus(OutboxStatus.PENDING.getCode());
        Date now = new Date();
        message.setCreateTime(now);
        message.setUpdateTime(now);
        outboxDao.insert(message);
        return message.getMessageId();
    }

    /** {@inheritDoc} */
    @Override
    public OutboxMessage getById(String messageId) {
        return outboxDao.selectById(messageId);
    }

    /** {@inheritDoc} */
    @Override
    public List<OutboxMessage> listForResend(int limit) {
        Date now = new Date();
        Date createdBefore = new Date(now.getTime() - RESEND_DELAY_MILLIS);
        Date claimedBefore = new Date(now.getTime() - CLAIM_TIMEOUT_MILLIS);
        return outboxDao.selectList(new LambdaQueryWrapper<OutboxMessage>()
                // 两段条件必须用 and(...) 包成一个整体，否则 or 会与外层条件平铺，拼出的 SQL 语义会变
                .and(w -> w
                        .in(OutboxMessage::getMessageStatus,
                                OutboxStatus.PENDING.getCode(), OutboxStatus.ERROR.getCode())
                        .le(OutboxMessage::getCreateTime, createdBefore)
                        .or(o -> o
                                .eq(OutboxMessage::getMessageStatus, OutboxStatus.CLAIMED.getCode())
                                .le(OutboxMessage::getUpdateTime, claimedBefore)))
                .orderByAsc(OutboxMessage::getCreateTime)
                // 只取一批候选、不需要总数：分页插件会额外发一次 count 查询
                // limit 取自 OutboxResendTask.RESEND_BATCH 常量，无外部输入
                .last("limit " + limit));
    }

    /** {@inheritDoc} */
    @Override
    public boolean claim(String messageId) {
        return outboxDao.update(null, new LambdaUpdateWrapper<OutboxMessage>()
                .eq(OutboxMessage::getMessageId, messageId)
                .in(OutboxMessage::getMessageStatus,
                        OutboxStatus.PENDING.getCode(), OutboxStatus.ERROR.getCode())
                .set(OutboxMessage::getMessageStatus, OutboxStatus.CLAIMED.getCode())
                .set(OutboxMessage::getUpdateTime, new Date())) > 0;
    }

    /** {@inheritDoc} */
    @Override
    public void markSent(String messageId) {
        boolean updated = outboxDao.update(null, new LambdaUpdateWrapper<OutboxMessage>()
                .eq(OutboxMessage::getMessageId, messageId)
                .in(OutboxMessage::getMessageStatus,
                        OutboxStatus.PENDING.getCode(), OutboxStatus.CLAIMED.getCode())
                .set(OutboxMessage::getMessageStatus, OutboxStatus.SENT.getCode())
                .set(OutboxMessage::getUpdateTime, new Date())) > 0;
        if (!updated) {
            log.warn("消息未置为已发送，可能已被 return 标成错误或已处理过，messageId={}", messageId);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void markError(String messageId, String cause) {
        outboxDao.update(null, new LambdaUpdateWrapper<OutboxMessage>()
                .eq(OutboxMessage::getMessageId, messageId)
                .in(OutboxMessage::getMessageStatus,
                        OutboxStatus.PENDING.getCode(), OutboxStatus.CLAIMED.getCode(),
                        OutboxStatus.SENT.getCode())
                .set(OutboxMessage::getMessageStatus, OutboxStatus.ERROR.getCode())
                .set(OutboxMessage::getUpdateTime, new Date()));
        log.error("消息投递失败，等待重投，messageId={}，原因={}", messageId, cause);
    }

    /** {@inheritDoc} */
    @Override
    public int removeSentBefore(Date deadline) {
        return outboxDao.delete(new LambdaQueryWrapper<OutboxMessage>()
                .eq(OutboxMessage::getMessageStatus, OutboxStatus.SENT.getCode())
                .le(OutboxMessage::getUpdateTime, deadline));
    }
}
