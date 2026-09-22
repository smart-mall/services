package product.mq;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.stereotype.Component;
import product.entity.MqMessageEntity;
import product.service.MqMessageService;

import java.util.List;

/**
 * 本地消息表（outbox）的投递端。
 *
 * <p><b>状态由 confirm / return 回调决定，不由 {@code convertAndSend} 的返回决定。</b>
 * 发送是异步的：方法正常返回只代表消息交给了客户端，broker 可能压根没收到、或者收到了但
 * 没有任何队列匹配。如果按方法返回就置「已发送」，定时任务永远不会重投 —— 消息永久丢失，
 * 正是 outbox 要防的事。</p>
 *
 * @see product.service.MqMessageService 数据库那一半
 */
@Slf4j
@Component
public class MqMessageSender {

    private final RabbitTemplate rabbitTemplate;
    private final MqMessageService mqMessageService;

    public MqMessageSender(ConnectionFactory connectionFactory,
                          MessageConverter jsonMessageConverter,
                          MqMessageService mqMessageService) {
        this.mqMessageService = mqMessageService;

        // 【为什么自己 new 一个 RabbitTemplate，而不是注入容器里那个】
        //
        // RabbitTemplate 的 confirm 回调和 returns 回调各自只能设一个，第二次设置会**直接抛异常**：
        //   IllegalStateException: Only one ConfirmCallback is supported by each RabbitTemplate
        // 它不是"后设的覆盖先设的"。而 common 的 RabbitConfig 已经在 @PostConstruct 里把两个槽位
        // 都占了（那份配置是给全部 11 个服务用的，只打 debug 日志），所以往容器里那个共享模板上
        // 再设一次，会让 product **启动直接失败**（已经被线上验证过一次了）。
        //
        // 这里用同一个 ConnectionFactory 建一个 outbox 专用的模板：**不注册成 bean**，
        // 所以既不会和容器里那个抢注入（RabbitConfig 是按类型注入 RabbitTemplate 的），
        // 也不受它单槽位的限制。
        //
        // publisher-confirm-type / publisher-returns 是配在 ConnectionFactory 上的
        // （application-common.yaml），共享生效；只有 mandatory 是模板级的，要显式设。
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter);
        template.setMandatory(true);
        template.setConfirmCallback((correlationData, ack, cause) -> onConfirm(correlationData, ack, cause));
        template.setReturnsCallback(this::onReturn);
        this.rabbitTemplate = template;
    }

    /**
     * 把一条消息投出去。<b>只负责发，不改状态</b> —— 状态交给回调。
     *
     * <p>刻意不抛异常：投递失败时状态留在「待投递」，定时任务会重投。往上抛只会让业务事务
     * 回滚，而业务数据本身是好的。</p>
     */
    public void send(MqMessageEntity message) {
        String messageId = message.getMessageId();
        try {
            Object payload = JSON.parseObject(message.getContent(), Class.forName(message.getClassType()));

            rabbitTemplate.convertAndSend(message.getToExchange(), message.getRoutingKey(), payload,
                    // 自己设 correlationId，不依赖 Spring 是否把 CorrelationData 的 id 带进消息属性：
                    // return 回调只能从消息属性里拿 id，拿不到就没法把记录标成「错误抵达」
                    amqpMessage -> {
                        amqpMessage.getMessageProperties().setCorrelationId(messageId);
                        return amqpMessage;
                    },
                    new CorrelationData(messageId));
        } catch (ClassNotFoundException e) {
            // 类都找不到，重投一万次也发不出去，直接标成错误等人工处理
            mqMessageService.markError(messageId, "消息体类型不存在: " + message.getClassType());
            log.error("消息体类型不存在，无法投递，messageId={}，classType={}", messageId, message.getClassType(), e);
        } catch (Exception e) {
            // 连接失败之类：状态留在「待投递」，等定时任务重投
            log.error("投递消息失败，等待定时任务重投，messageId={}", messageId, e);
        }
    }

    /** 按 messageId 投一条。业务事务的 {@code afterCommit} 用这个 */
    public void send(String messageId) {
        MqMessageEntity message = mqMessageService.getById(messageId);
        if (message == null) {
            log.warn("消息不存在，可能已被清理，messageId={}", messageId);
            return;
        }
        send(message);
    }

    /**
     * 扫出待投递/错误抵达的消息重投一轮，返回处理条数。
     *
     * <p>重投必然导致重复投递（broker 确认了但本地状态没来得及改就崩了），这是 at-least-once，
     * 消不掉也不该消 —— <b>宁可重复，不可丢失</b>，重复由消费端幂等兜住。</p>
     */
    public int resendPending(int limit) {
        List<MqMessageEntity> pending = mqMessageService.listPendingForResend(limit);
        pending.forEach(this::send);
        return pending.size();
    }

    /** broker 逐条确认。{@code ack=true} 只代表"交换机收下了" */
    private void onConfirm(CorrelationData correlationData, boolean ack, String cause) {
        if (correlationData == null || correlationData.getId() == null) {
            log.warn("收到没有 correlationData 的确认回调，无法定位消息，ack={}", ack);
            return;
        }
        String messageId = correlationData.getId();
        if (ack) {
            mqMessageService.markSent(messageId);
        } else {
            mqMessageService.markError(messageId, "broker nack: " + cause);
        }
    }

    /**
     * 消息被退回：交换机收下了，但没有任何队列匹配。
     *
     * <p>这种情况 broker <b>照样会 ack</b>，所以必须单独处理 —— 否则就是"标记成已发送，
     * 消息其实已经被丢弃了"。典型触发场景：消费方从没启动过，队列和 binding 都还不存在。</p>
     */
    private void onReturn(ReturnedMessage returned) {
        String messageId = returned.getMessage().getMessageProperties().getCorrelationId();
        String cause = "消息不可路由: exchange=" + returned.getExchange()
                + ", routingKey=" + returned.getRoutingKey()
                + ", replyCode=" + returned.getReplyCode()
                + ", replyText=" + returned.getReplyText();

        if (messageId == null) {
            // 认不出是哪条。什么都不做是最安全的：记录留在「待投递」，定时任务还会重投，
            // 等队列建好自然就投进去了。这里只把现场打出来
            log.error("消息被退回但拿不到 correlationId，无法更新状态，{}", cause);
            return;
        }
        mqMessageService.markError(messageId, cause);
    }
}
