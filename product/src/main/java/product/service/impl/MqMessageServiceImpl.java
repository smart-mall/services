package product.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import product.constant.MqMessageStatus;
import product.dao.MqMessageDao;
import product.entity.MqMessageEntity;
import product.service.MqMessageService;

import java.util.Date;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service("mqMessageService")
public class MqMessageServiceImpl extends ServiceImpl<MqMessageDao, MqMessageEntity> implements MqMessageService {

    /**
     * 定时任务只捞创建满 1 分钟的消息。
     *
     * <p>刚提交的那条正由 {@code afterCommit} 投递，定时任务和它抢同一条只会产生一次无谓的
     * 重复投递 —— 重复本身无害（消费端幂等），这里只是想少做无用功。</p>
     */
    private static final long RESEND_DELAY_MILLIS = 60_000L;

    @Override
    public String savePending(String toExchange, String routingKey, Object payload) {
        MqMessageEntity message = new MqMessageEntity();
        message.setMessageId(UUID.randomUUID().toString().replace("-", ""));
        message.setContent(JSON.toJSONString(payload));
        message.setToExchange(toExchange);
        message.setRoutingKey(routingKey);
        // 重投时靠它反序列化回对象。存全限定类名而不是 Class 对象，是因为跨服务、跨进程
        message.setClassType(payload.getClass().getName());
        message.setMessageStatus(MqMessageStatus.PENDING.getCode());

        Date now = new Date();
        message.setCreateTime(now);
        message.setUpdateTime(now);

        this.save(message);
        return message.getMessageId();
    }

    @Override
    public void markSent(String messageId) {
        boolean updated = this.update(new LambdaUpdateWrapper<MqMessageEntity>()
                .eq(MqMessageEntity::getMessageId, messageId)
                // 条件里带 status=0：return 回调可能比 confirm 先到并把状态改成"错误抵达"，
                // 后到的 ack 不能把它改回"已发送"，否则这条消息就再也不会被重投了
                .eq(MqMessageEntity::getMessageStatus, MqMessageStatus.PENDING.getCode())
                .set(MqMessageEntity::getMessageStatus, MqMessageStatus.SENT.getCode())
                .set(MqMessageEntity::getUpdateTime, new Date()));

        if (!updated) {
            log.warn("消息状态未从「待投递」置为「已发送」，可能已被 return 标成错误或已处理过，messageId={}", messageId);
        }
    }

    @Override
    public void markError(String messageId, String cause) {
        this.update(new LambdaUpdateWrapper<MqMessageEntity>()
                .eq(MqMessageEntity::getMessageId, messageId)
                .in(MqMessageEntity::getMessageStatus,
                        MqMessageStatus.PENDING.getCode(), MqMessageStatus.SENT.getCode())
                .set(MqMessageEntity::getMessageStatus, MqMessageStatus.ERROR.getCode())
                .set(MqMessageEntity::getUpdateTime, new Date()));

        log.error("消息投递失败，等待定时重投，messageId={}，原因={}", messageId, cause);
    }

    @Override
    public List<MqMessageEntity> listPendingForResend(int limit) {
        Date deadline = new Date(System.currentTimeMillis() - RESEND_DELAY_MILLIS);

        return this.page(new Page<>(1, limit), new LambdaQueryWrapper<MqMessageEntity>()
                        .in(MqMessageEntity::getMessageStatus,
                                MqMessageStatus.PENDING.getCode(), MqMessageStatus.ERROR.getCode())
                        .le(MqMessageEntity::getCreateTime, deadline)
                        .orderByAsc(MqMessageEntity::getCreateTime))
                .getRecords();
    }

    @Override
    public int removeSentBefore(Date deadline) {
        return this.getBaseMapper().delete(new LambdaQueryWrapper<MqMessageEntity>()
                .eq(MqMessageEntity::getMessageStatus, MqMessageStatus.SENT.getCode())
                .le(MqMessageEntity::getUpdateTime, deadline));
    }
}
