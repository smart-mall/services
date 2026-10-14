package order.constant;

import java.math.BigDecimal;

/** 订单服务的常量：防重令牌、测试支付票据、订单来源、自动确认天数与赠送比例。 */
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

    /**
     * 自动确认收货的天数，下单时写进 {@code oms_order.auto_confirm_day}。
     *
     * <p>普通订单与秒杀单都写这个值，集中在这里以免两条建单路径各写一遍而漂移。
     */
    public static final int AUTO_CONFIRM_DAY = 7;

    /**
     * 退货地址与收货人：退货申请审核通过时写进申请单，会员照着这个地址寄回。
     *
     * <p><b>为什么不建配置表、也不让管理员每次手填：</b>项目只有一处售后收货地址、几乎不会变。
     * 让管理员手填等于每单采集一个没有来源的数据（每次填的都可能不一样，出事时无从复现），
     * 与"物流单号由服务端生成、管理员不填"同源。
     *
     * <p><b>为什么不从 {@code wms_ware_info} 取：</b>那张表里确实有仓库地址，但取它要给退货审核
     * 加上一个 ware 的远程依赖 —— ware 不可用会把审核一起挡住，而这是个纯展示信息。
     * 以后真有多仓、要按库存所在仓退货时再改成查仓库，届时这个常量就该消失。
     */
    public static final String RETURN_COMPANY_ADDRESS = "河北省石家庄市长安区中山东路 1 号 谷粒商城售后仓";

    /** 商家的退货收货人，随 {@link #RETURN_COMPANY_ADDRESS} 一起写进申请单。 */
    public static final String RETURN_RECEIVER = "谷粒商城售后仓";

    /** 商家的退货收货电话，随 {@link #RETURN_COMPANY_ADDRESS} 一起写进申请单。 */
    public static final String RETURN_RECEIVER_PHONE = "0311-88880000";

    /**
     * 退款流水的状态：已退款。
     *
     * <p>本项目没有真实退款通道，平台的应答就是最终结果，所以只有这一个取值 ——
     * 申请被拒绝时不写流水（没有钱出去），因此也不需要"已拒绝"这一档。
     */
    public static final int REFUND_STATUS_SUCCESS = 1;

    /**
     * 下单赠送积分与成长值的基础比例：订单项原价总额的 10%。
     *
     * <p>这是"下单算好、付款入账"的那份基础量；商品上另可用 {@code sms_spu_bounds} 配一份
     * 固定加量，两者在下单时相加写进订单项快照。没配过积分设置的商品只拿这一份，不额外加量。
     *
     * <p>取整一律向下，与本项目既有的 {@code intValue()} 口径一致。
     */
    public static final BigDecimal GIFT_BASE_RATE = new BigDecimal("0.1");

}
