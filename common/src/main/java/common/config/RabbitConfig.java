package common.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
@Slf4j
public class RabbitConfig {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    /**
     * 消息转换器。发和收都靠它，但<b>必须声明成 bean</b>，不能只在下面 initRabbitTemplate 里
     * 往 RabbitTemplate 上 set 一下就完事 —— 那是两套东西：
     *
     * <p>{@code rabbitTemplate.setMessageConverter(...)} 只管发出去的模板。消费端是监听容器，
     * 由 Spring Boot 的 {@code SimpleRabbitListenerContainerFactoryConfigurer} 去容器里找
     * {@code MessageConverter} bean 来配（且要求唯一，多个就不配）。没有这个 bean 的时候，
     * 消费端退回默认的 {@code SimpleMessageConverter}，它看到 {@code contentType: application/json}
     * 只会把 body 当字节数组返回。</p>
     *
     * <p>后果很隐蔽：{@code @RabbitHandler} 是按转换后的类型找方法的，拿到 {@code byte[]} 就抛
     * {@code NoSuchMethodException: No listener method found ... for class [B}，消息被直接丢弃，
     * 而发消息那一侧的 HTTP 接口全是 200。秒杀下单、取消订单释放库存、超时关单全都在这里静默失效。</p>
     *
     * <p><b>为什么必须是 static</b>：不 static 会形成环，整个服务起不来 ——
     * {@code RabbitConfig} 注入 {@code RabbitTemplate}，而 {@code RabbitTemplate} 的创建又要经过
     * {@code RabbitTemplateConfigurer} 去取这个 {@code MessageConverter}，取它就要求先实例化
     * {@code RabbitConfig}，绕回来了。Spring Boot 默认禁止循环引用，直接
     * {@code APPLICATION FAILED TO START}。static 的 {@code @Bean} 不需要先实例化所在配置类，
     * 环就断在这里。改回非 static 会让 common 的所有下游服务全部启动失败。</p>
     */
    @Bean
    public static MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @PostConstruct
    public void initRabbitTemplate() {
        // 转换器不在这里设：上面那个 bean 是容器里唯一的 MessageConverter，
        // Spring Boot 的 RabbitAutoConfiguration 会把它同时装到 RabbitTemplate 和监听容器上。
        // 这里再 set 一遍反而会 new 出第二个实例（static 方法不经过 CGLIB 代理）。

        // 设置确认回调
        rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
            log.debug("confirm...correlationData[{}]==>ack:[{}]==>cause:[{}]", correlationData, ack, cause);
        });

        // 设置返回回调
        rabbitTemplate.setReturnsCallback(returnCallback -> {
            log.debug("ReturnsCallback...returnedMessage:[{}]==>replyCode:[{}]==>replyText:[{}]==>exchange:[{}]==>routingKey:[{}]", returnCallback.getMessage(), returnCallback.getReplyCode(), returnCallback.getReplyText(), returnCallback.getExchange(), returnCallback.getRoutingKey());
        });
    }
}
