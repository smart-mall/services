package product.config;

import common.mq.MqBuilder;
import common.mq.MqConstant;
import org.springframework.amqp.core.Exchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * product 自己的 topic 交换机声明。
 *
 * <p>只声明交换机，队列与 binding 由消费方（coupon / third-party / ware / search）各自声明并绑上来，
 * product 因此不需要知道有哪些消费者。代价是消费方从未启动时其队列不存在、消息不可路由，靠
 * {@code publisher-returns} 把消息退回并标成「错误抵达」，再由本地消息表定时重投兜住。
 *
 * <p>消费方也会声明同名交换机，AMQP 声明幂等，但参数必须完全一致，否则 406 PRECONDITION_FAILED。
 */
@Configuration
public class RabbitMQConfig {

    /**
     * 声明 product 事件交换机。
     *
     * @return 持久化、非自动删除的 topic 交换机
     */
    @Bean
    public Exchange productExchange() {
        return MqBuilder.topicExchange(MqConstant.Exchanges.PRODUCT);
    }
}
