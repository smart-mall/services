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
 * 两个 RabbitTemplate（尽力而为 / 至少一次）+ MqPublisher。
 *
 * <p>两个模板不能合并：confirm 与 returns 回调各只有一个槽位，第二次设置会抛
 * IllegalStateException，而两种投递语义要的回调不同。</p>
 *
 * <p>必须排在 RabbitAutoConfiguration 之前，否则 Boot 还会建一个自己的 rabbitTemplate。</p>
 */
@Slf4j
@AutoConfiguration
@AutoConfigureBefore(RabbitAutoConfiguration.class)
public class MqAutoConfiguration {

    public static final String BEST_EFFORT_TEMPLATE = "bestEffortRabbitTemplate";
    public static final String RELIABLE_TEMPLATE = "reliableRabbitTemplate";

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

    /** 本地消息表专用，回调由对应实现设置 */
    @Bean(RELIABLE_TEMPLATE)
    public RabbitTemplate reliableRabbitTemplate(RabbitTemplateConfigurer configurer,
                                                ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate();
        configurer.configure(template, connectionFactory);
        return template;
    }

    @Bean
    public MqPublisher mqPublisher(@Qualifier(BEST_EFFORT_TEMPLATE) RabbitTemplate bestEffortRabbitTemplate) {
        return new BestEffortMqPublisher(bestEffortRabbitTemplate);
    }
}
