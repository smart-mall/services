package common.mq;

/**
 * MQ 投递门面。业务代码依赖本接口，不直接用 RabbitTemplate。
 */
public interface MqPublisher {

    /**
     * 尽力而为投递，不保证送达。
     *
     * @param exchange   交换机名，取 {@link MqConstant.Exchanges}
     * @param routingKey 路由键，取 {@link MqConstant.RoutingKeys}
     * @param payload    消息体
     */
    void publish(String exchange, String routingKey, Object payload);
}
