package common.config;

import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 消息转换器装配：把消息体按 JSON 收发。
 *
 * <p>不带配置前缀，由 common 的 {@code AutoConfiguration.imports} 注册；
 * 模板与门面的装配见 {@code common.mq.MqAutoConfiguration}。
 */
@Configuration
public class RabbitConfig {

    /**
     * 必须是 bean：消费端监听容器由 Spring Boot 从容器里取唯一的 MessageConverter 来配，
     * 缺失时会退回 SimpleMessageConverter，把 json body 当 byte[]，
     * {@code @RabbitHandler} 按类型匹配不上方法，消息被静默丢弃。
     *
     * <p>保持 static，避免在 MessageConverter 这条早期依赖链上实例化本配置类。</p>
     */
    @Bean
    public static MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
