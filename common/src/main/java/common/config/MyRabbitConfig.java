package common.config;

import jakarta.annotation.PostConstruct;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;


@Configuration
public class MyRabbitConfig {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @PostConstruct
    public void initRabbitTemplate() {
        // 设置消息转换器
        rabbitTemplate.setMessageConverter(new Jackson2JsonMessageConverter());

        // 设置确认回调
        rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
            System.out.println("confirm...correlationData[" + correlationData + "]==>ack:[" + ack + "]==>cause:[" + cause + "]");
        });

        // 设置返回回调
        rabbitTemplate.setReturnsCallback(returnCallback -> {
            System.out.println("ReturnsCallback...returnedMessage:[" + returnCallback.getMessage() + "]==>replyCode:[" + returnCallback.getReplyCode() + "]==>replyText:[" + returnCallback.getReplyText() + "]==>exchange:[" + returnCallback.getExchange() + "]==>routingKey:[" + returnCallback.getRoutingKey() + "]");
        });
    }
}
