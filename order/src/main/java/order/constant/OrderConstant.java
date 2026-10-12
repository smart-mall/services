package order.constant;

/** 订单服务的常量：防重令牌、测试支付票据与订单来源。 */
public class OrderConstant {

    /** 防重令牌的 Redis 键前缀，实际键为前缀拼接 memberId。 */
    public static final String USER_ORDER_TOKEN_PREFIX = "order:token";

    /** 测试支付票据的 Redis 键前缀，实际键为前缀拼接订单号。 */
    public static final String MOCK_PAY_TICKET_PREFIX = "order:mock-pay:ticket:";

    /**
     * 测试支付票据的有效期（分钟）。
     *
     * <p>票据只需覆盖用户从支付页跳到测试收银台并点击确认的时间。订单自身 1 分钟后即被超时关单，
     * 票据即使更长寿也过不了状态复核，因此放宽到分钟级不产生额外敞口。
     */
    public static final int MOCK_PAY_TICKET_TIMEOUT_MINUTES = 5;

    /**
     * 防重令牌的有效期（分钟）。
     *
     * <p>令牌在结算页下发，用户填完地址点提交时它仍需有效；而关单延迟队列的 TTL 只有 1 分钟
     * （见 {@link order.config.RabbitMQConfig#orderCreatedDelayQueue()}），令牌有效期不得短于它。
     */
    public static final int USER_ORDER_TOKEN_TIMEOUT_MINUTES = 30;

    /**
     * 订单来源：PC 订单，对应 {@code oms_order.source_type = 0}。
     *
     * <p>会员端只有浏览器 SPA 一个入口，不区分 App，因此订单来源恒为 PC 订单。
     */
    public static final int SOURCE_TYPE_PC = 0;

}
