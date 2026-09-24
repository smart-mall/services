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
 * 本地消息表的装配。默认关闭，服务需要设置 gl.mq.outbox.enabled=true 才生效
 * —— 它要求该服务的库里有 mq_message 表。
 */
@Slf4j
@AutoConfiguration
@ConditionalOnProperty(prefix = "gl.mq.outbox", name = "enabled", havingValue = "true")
@MapperScan(basePackages = "common.mq.outbox", annotationClass = Mapper.class)
public class OutboxAutoConfiguration {

    public static final String RELIABLE_TEMPLATE = "reliableRabbitTemplate";

    @Bean
    public OutboxStore outboxStore(OutboxDao outboxDao) {
        return new OutboxStoreImpl(outboxDao);
    }

    /** 至少一次的模板：它的 confirm / returns 回调驱动本地消息表的状态 */
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

    @Bean
    public OutboxPublisher outboxPublisher(
            @Qualifier(RELIABLE_TEMPLATE) RabbitTemplate reliableRabbitTemplate,
            OutboxStore outboxStore,
            ObjectMapper objectMapper) {
        return new OutboxPublisher(reliableRabbitTemplate, outboxStore, objectMapper);
    }

    @Bean
    public ReliableMqPublisher reliableMqPublisher(OutboxPublisher outboxPublisher) {
        return new ReliableMqPublisher(outboxPublisher);
    }

    @Bean
    public OutboxResendTask outboxResendTask(OutboxPublisher outboxPublisher, OutboxStore outboxStore) {
        return new OutboxResendTask(outboxPublisher, outboxStore);
    }
}
