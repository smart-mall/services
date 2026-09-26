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
 * 尽力而为投递的自动装配：{@link RabbitTemplate} 与 {@link MqPublisher} 门面。
 *
 * <p>必须排在 {@link RabbitAutoConfiguration} 之前，否则 Boot 还会建一个自己的 rabbitTemplate。
 *
 * <p>至少一次的那个模板归本地消息表管，在 {@link common.mq.outbox.OutboxAutoConfiguration}。</p>
 */
@Slf4j
@AutoConfiguration
@AutoConfigureBefore(RabbitAutoConfiguration.class)
public class MqAutoConfiguration {

    /** 尽力而为模板的 bean 名，注入点用 {@code @Qualifier} 引用它。 */
    public static final String BEST_EFFORT_TEMPLATE = "bestEffortRabbitTemplate";

    /**
     * 创建尽力而为的 {@link RabbitTemplate}：confirm 与 returns 回调只打 debug 日志，不驱动任何状态。
     *
     * @param configurer        Boot 的模板配置器，负责套用 {@code spring.rabbitmq.*} 配置
     * @param connectionFactory RabbitMQ 连接工厂
     * @return 尽力而为模板，投递结果不落库
     */
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

    /**
     * 创建默认的 {@link MqPublisher} 门面，供按类型注入 {@code MqPublisher} 的业务代码使用。
     *
     * <p>本地消息表那条实现是另一个类型，按类型注入不会歧义。
     *
     * @param bestEffortRabbitTemplate 尽力而为模板
     * @return 尽力而为的投递门面
     */
    @Bean
    @Primary
    public MqPublisher mqPublisher(@Qualifier(BEST_EFFORT_TEMPLATE) RabbitTemplate bestEffortRabbitTemplate) {
        return new BestEffortMqPublisher(bestEffortRabbitTemplate);
    }
}
