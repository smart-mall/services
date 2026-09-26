package common.mq;

/**
 * MQ 投递门面，业务代码依赖本接口而不直接用 RabbitTemplate。
 *
 * <p>实现有两种可靠性语义：尽力而为（失败只记日志）与至少一次（本地消息表 + 重投），
 * 调用方按业务需要注入对应的实现。
 */
public interface MqPublisher {

    /**
     * 尽力而为投递，不保证送达。
     *
     * <p>{@code payload} 必须能被序列化：至少一次的实现会先把它写成 JSON 落库，序列化失败直接抛异常，
     * 不会留下消息记录。
     *
     * @param exchange   交换机名，取 {@link MqConstant.Exchanges}
     * @param routingKey 路由键，取 {@link MqConstant.RoutingKeys}；带通配符的绑定模式不能当路由键
     * @param payload    消息体
     */
    void publish(String exchange, String routingKey, Object payload);
}
