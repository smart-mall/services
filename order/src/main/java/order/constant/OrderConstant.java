package order.constant;

public class OrderConstant {

    public static final String USER_ORDER_TOKEN_PREFIX = "order:token";

    /**
     * 防重令牌的有效期（分钟）。
     *
     * <p>必须不小于"关单超时"：令牌是确认页发的，用户填完地址、点提交时它还得有效。
     * 原来写死 30 分钟，而关单 TTL 是 1 分钟（见 RabbitMQConfig），两者是脱节的。</p>
     */
    public static final int USER_ORDER_TOKEN_TIMEOUT_MINUTES = 30;

}
