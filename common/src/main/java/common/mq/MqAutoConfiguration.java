package common.mq;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.autoconfigure.amqp.RabbitTemplateConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * 尽力而为的 RabbitTemplate + MqPublisher 门面。
 * 至少一次那个模板归本地消息表管，在 {@code common.mq.outbox.OutboxAutoConfiguration}。
 *
 * <p>必须排在 RabbitAutoConfiguration 之前，否则 Boot 还会建一个自己的 rabbitTemplate。</p>
 */
@Slf4j
@AutoConfiguration
@AutoConfigureBefore(RabbitAutoConfiguration.class)
public class MqAutoConfiguration {

    public static final String BEST_EFFORT_TEMPLATE = "bestEffortRabbitTemplate";

    /** 默认模板：回调只打日志 */
    @Bean(BEST_EFFORT_TEMPLATE)
    @Primary
    public RabbitTemplate bestEffortRabbitTemplate(RabbitTemplateConfigurer configurer,
                                                  ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate();
        configurer.configure(template, connectionFactory);

        template.setConfirmCallback((correlationData, ack, cause) ->
                log.debug("confirm...correlationData[{}]==>ack:[{}]==>cause:[{}]", correlationData, ack, cause));

        template.setReturnsCallback(returned ->
                log.debug("ReturnsCallback...returnedMessage:[{}]==>replyCode:[{}]==>replyText:[{}]==>exchange:[{}]==>routingKey:[{}]",
                        returned.getMessage(), returned.getReplyCode(), returned.getReplyText(),
                        returned.getExchange(), returned.getRoutingKey()));

        return template;
    }

    /** 默认门面；本地消息表那条是另一个类型，按类型注入不会歧义 */
    @Bean
    @Primary
    public MqPublisher mqPublisher(@Qualifier(BEST_EFFORT_TEMPLATE) RabbitTemplate bestEffortRabbitTemplate) {
        return new BestEffortMqPublisher(bestEffortRabbitTemplate);
    }
}
