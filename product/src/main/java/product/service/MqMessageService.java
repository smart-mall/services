package product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import product.entity.MqMessageEntity;

import java.util.Date;
import java.util.List;

/**
 * 本地消息表（outbox）。
 *
 * <p>这个接口只做数据库那一半，投递那一半在 {@link product.mq.MqMessageSender} ——
 * 分工是刻意的：{@code savePending} 必须能被业务事务调用（要和业务数据同事务），
 * 而投递必须在事务提交之后，两者生命周期不同，放一个类里容易被人误用。</p>
 */
public interface MqMessageService extends IService<MqMessageEntity> {

    /**
     * 落一条待投递消息，返回 messageId。
     *
     * <p><b>必须在业务事务里调用</b>，这是整套可靠消息的地基：提交成功就意味着
     * "业务改完了"和"有一条消息待投递"同时成立，没有中间态。</p>
     */
    String savePending(String toExchange, String routingKey, Object payload);

    /** broker 确认收到。只允许 {@code 待投递 → 已发送}，防止后到的 ack 把"错误抵达"改回来 */
    void markSent(String messageId);

    /** 被 return（不可路由）或 nack。允许 {@code 待投递/已发送 → 错误抵达} */
    void markError(String messageId, String cause);

    /** 待重投的消息：状态为待投递/错误抵达，且创建时间早于 {@code now - 1 分钟} */
    List<MqMessageEntity> listPendingForResend(int limit);

    /** 清理已发送成功且超过保留期的记录，返回删除条数 */
    int removeSentBefore(Date deadline);
}
