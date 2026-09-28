package order.constant;

/** 订单防重令牌的常量：Redis 键前缀与令牌有效期。 */
public class OrderConstant {

    /** 防重令牌的 Redis 键前缀，实际键为前缀拼接 memberId。 */
    public static final String USER_ORDER_TOKEN_PREFIX = "order:token";

    /**
     * 防重令牌的有效期（分钟）。
     *
     * <p>令牌在结算页下发，用户填完地址点提交时它仍需有效；而关单延迟队列的 TTL 只有 1 分钟
     * （见 {@link order.config.RabbitMQConfig#orderCreatedDelayQueue()}），令牌有效期不得短于它。
     */
    public static final int USER_ORDER_TOKEN_TIMEOUT_MINUTES = 30;

}
