package order.constant;

/**
 * 支付方式常量，取值与 {@code oms_order.pay_type} 一致。
 */
public class PayConstant {

    /** 支付宝，对应 {@code pay_type = 1}。 */
    public static final Integer ALIPAY = 1;

    /** 微信支付，对应 {@code pay_type = 2}。 */
    public static final Integer WXPAY = 2;

}
