package order.constant;

/**
 * 支付方式常量，取值与 {@code oms_order.pay_type} 一致。
 */
public class PayConstant {

    /** 支付宝，对应 {@code pay_type = 1}。 */
    public static final Integer ALIPAY = 1;

    /** 微信支付，对应 {@code pay_type = 2}。 */
    public static final Integer WXPAY = 2;

    /**
     * 货到付款，对应 {@code pay_type = 4}。
     *
     * <p>本项目还没有货到付款链路，这个取值只用于换算库存工作单的付款方式，不会被下单流程写入。
     */
    public static final Integer CASH_ON_DELIVERY = 4;

}
