package thirdParty.config;

import common.mq.MqBuilder;
import common.mq.MqConstant;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * third-party 侧消费 {@code product.deleted} 所需的交换机与队列，用来清 MinIO 里的孤儿文件。
 *
 * <p>结构和 coupon 侧完全对称（业务队列 → DLX → TTL 重试队列 → 回到业务队列；重试到上限由
 * 监听器投进死信队列），只是换了前缀。<b>两个服务各自一个队列、各自消费同一条消息</b>，
 * 互不阻塞：coupon 挂了不影响清文件，反过来的道理也一样。</p>
 *
 * <p>所有名称取自 {@link MqConstant}；和 product 侧的交换机声明引用同一个常量，
 * "同名同参数"由代码保证。</p>
 */
@Configuration
public class RabbitMQConfig {

    @Bean
    public Exchange productEventExchange() {
        return MqBuilder.topicExchange(MqConstant.Exchanges.PRODUCT_EVENT);
    }

    /** 业务队列：消费失败 nack 后由自己的 DLX 接走 */
    @Bean
    public Queue thirdPartyProductDeletedQueue() {
        return MqBuilder.deadLetterQueue(
                MqConstant.Queues.THIRDPARTY_PRODUCT_DELETED,
                MqConstant.Exchanges.THIRDPARTY_PRODUCT_DELETED_DLX,
                MqConstant.RoutingKeys.THIRDPARTY_PRODUCT_DELETED_RETRY);
    }

    /** product.deleted → 业务队列 */
    @Bean
    public Binding thirdPartyProductDeletedBinding() {
        return MqBuilder.bind(
                MqConstant.Queues.THIRDPARTY_PRODUCT_DELETED,
                MqConstant.Exchanges.PRODUCT_EVENT,
                MqConstant.RoutingKeys.PRODUCT_DELETED);
    }

    @Bean
    public Exchange thirdPartyProductDeletedDlx() {
        return MqBuilder.directExchange(MqConstant.Exchanges.THIRDPARTY_PRODUCT_DELETED_DLX);
    }

    /** 消息在这里躺 1 分钟，到期死信回业务交换机，等于延迟重投 */
    @Bean
    public Queue thirdPartyProductDeletedRetryQueue() {
        return MqBuilder.ttlQueue(
                MqConstant.Queues.THIRDPARTY_PRODUCT_DELETED_RETRY,
                MqConstant.Exchanges.PRODUCT_EVENT,
                MqConstant.RoutingKeys.PRODUCT_DELETED,
                MqConstant.TtlMillis.PRODUCT_DELETED_RETRY);
    }

    /** 重试路由键 → 重试队列 */
    @Bean
    public Binding thirdPartyProductDeletedRetryBinding() {
        return MqBuilder.bind(
                MqConstant.Queues.THIRDPARTY_PRODUCT_DELETED_RETRY,
                MqConstant.Exchanges.THIRDPARTY_PRODUCT_DELETED_DLX,
                MqConstant.RoutingKeys.THIRDPARTY_PRODUCT_DELETED_RETRY);
    }

    /** 死信队列。没有消费者，等人工处理 */
    @Bean
    public Queue thirdPartyProductDeletedDlq() {
        return MqBuilder.durableQueue(MqConstant.Queues.THIRDPARTY_PRODUCT_DELETED_DLQ);
    }

    /** 死信队列的绑定：路由键与队列名同值（沿用既有拓扑） */
    @Bean
    public Binding thirdPartyProductDeletedDlqBinding() {
        return MqBuilder.bind(
                MqConstant.Queues.THIRDPARTY_PRODUCT_DELETED_DLQ,
                MqConstant.Exchanges.THIRDPARTY_PRODUCT_DELETED_DLX,
                MqConstant.Queues.THIRDPARTY_PRODUCT_DELETED_DLQ);
    }
}
