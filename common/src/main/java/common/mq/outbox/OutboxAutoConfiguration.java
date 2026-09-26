package common.mq.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.annotations.Mapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.amqp.RabbitTemplateConfigurer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * 本地消息表的自动装配，默认关闭。
 *
 * <p>服务要设置 {@code gl.mq.outbox.enabled=true} 才生效 —— 它要求该服务的库里有 {@code mq_message} 表。
 *
 * <p>装配出的可靠模板、投递端与重投任务构成至少一次投递链路。
 */
@Slf4j
@AutoConfiguration
@ConditionalOnProperty(prefix = "gl.mq.outbox", name = "enabled", havingValue = "true")
@MapperScan(basePackages = "common.mq.outbox", annotationClass = Mapper.class)
public class OutboxAutoConfiguration {

    /** 至少一次模板的 bean 名，注入点用 {@code @Qualifier} 引用它。 */
    public static final String RELIABLE_TEMPLATE = "reliableRabbitTemplate";

    /**
     * 创建本地消息表读写实现。
     *
     * @param outboxDao 消息表 Mapper
     * @return 本地消息表读写实现
     */
    @Bean
    public OutboxStore outboxStore(OutboxDao outboxDao) {
        return new OutboxStoreImpl(outboxDao);
    }

    /**
     * 创建至少一次投递用的 {@link RabbitTemplate}：confirm / returns 回调直接改本地消息表的状态。
     *
     * <p>回调靠消息上的 CorrelationData id 定位记录，投递方必须带上，否则只能记日志。
     *
     * @param configurer        Boot 的模板配置器，负责套用 {@code spring.rabbitmq.*} 配置
     * @param connectionFactory RabbitMQ 连接工厂
     * @param outboxStore       本地消息表读写，回调据此标记已发送或错误
     * @return 可靠投递模板
     */
    @Bean(RELIABLE_TEMPLATE)
    public RabbitTemplate reliableRabbitTemplate(RabbitTemplateConfigurer configurer,
                                                ConnectionFactory connectionFactory,
                                                OutboxStore outboxStore) {
        RabbitTemplate template = new RabbitTemplate();
        configurer.configure(template, connectionFactory);

        template.setConfirmCallback((correlationData, ack, cause) -> {
            if (correlationData == null || correlationData.getId() == null) {
                log.warn("收到没有 correlationData 的确认回调，ack={}", ack);
                return;
            }
            if (ack) {
                outboxStore.markSent(correlationData.getId());
            } else {
                outboxStore.markError(correlationData.getId(), "broker nack: " + cause);
            }
        });

        // 交换机收下了但没有队列匹配。broker 照样 ack，所以必须单独处理
        template.setReturnsCallback(returned -> {
            String messageId = returned.getMessage().getMessageProperties().getCorrelationId();
            String cause = "消息不可路由: exchange=" + returned.getExchange()
                    + ", routingKey=" + returned.getRoutingKey()
                    + ", replyCode=" + returned.getReplyCode();
            if (messageId == null) {
                log.error("消息被退回但拿不到 correlationId，{}", cause);
                return;
            }
            outboxStore.markError(messageId, cause);
        });

        return template;
    }

    /**
     * 创建本地消息表的投递端。
     *
     * @param reliableRabbitTemplate 可靠投递模板，带 confirm / returns 回调
     * @param outboxStore            本地消息表读写
     * @param objectMapper           消息体 JSON 序列化器
     * @return 投递端
     */
    @Bean
    public OutboxPublisher outboxPublisher(
            @Qualifier(RELIABLE_TEMPLATE) RabbitTemplate reliableRabbitTemplate,
            OutboxStore outboxStore,
            ObjectMapper objectMapper) {
        return new OutboxPublisher(reliableRabbitTemplate, outboxStore, objectMapper);
    }

    /**
     * 创建至少一次语义的 {@link common.mq.MqPublisher} 实现，注入时按具体类型取用。
     *
     * @param outboxPublisher 本地消息表投递端
     * @return 至少一次投递门面
     */
    @Bean
    public ReliableMqPublisher reliableMqPublisher(OutboxPublisher outboxPublisher) {
        return new ReliableMqPublisher(outboxPublisher);
    }

    /**
     * 创建重投与清理的定时任务。
     *
     * @param outboxPublisher 本地消息表投递端
     * @param outboxStore     本地消息表读写
     * @return 定时任务 bean
     */
    @Bean
    public OutboxResendTask outboxResendTask(OutboxPublisher outboxPublisher, OutboxStore outboxStore) {
        return new OutboxResendTask(outboxPublisher, outboxStore);
    }
}
