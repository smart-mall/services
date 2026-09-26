package order.vo;

import lombok.Data;
import lombok.ToString;

import java.util.Date;

/**
 * 支付宝异步通知回传的原始参数。
 *
 * <p>字段名与支付宝表单字段完全一致（含下划线），不能按 Java 命名习惯改写，否则取不到值；
 * 全部是字符串，只有 {@link #notify_time} 由框架转成日期。</p>
 */
@ToString
@Data
public class PayAsyncVo {

    /** 交易创建时间，支付宝格式的字符串。 */
    private String gmt_create;

    /** 报文编码，如 utf-8。 */
    private String charset;

    /** 交易付款时间，支付宝格式的字符串。 */
    private String gmt_payment;

    /** 通知发送时间。 */
    private Date notify_time;

    /** 订单标题，下单时传的商品名。 */
    private String subject;

    /** 支付宝签名，验签用。 */
    private String sign;

    /** 买家支付宝用户号。 */
    private String buyer_id;

    /** 订单描述。 */
    private String body;

    /** 开票金额，即用户实际付款金额。 */
    private String invoice_amount;

    /** 接口版本号。 */
    private String version;

    /** 通知校验 ID，对账排查用。 */
    private String notify_id;

    /** 支付渠道金额明细。 */
    private String fund_bill_list;

    /** 通知类型，交易类通知固定为 trade_status_sync。 */
    private String notify_type;

    /** 商户订单号，即本系统的 orderSn。 */
    private String out_trade_no;

    /** 订单总金额。 */
    private String total_amount;

    /** 交易状态，TRADE_SUCCESS / TRADE_FINISHED 表示已收款。 */
    private String trade_status;

    /** 支付宝交易号。 */
    private String trade_no;

    /** 授权方的 app_id。 */
    private String auth_app_id;

    /** 商家实收金额。 */
    private String receipt_amount;

    /** 集分宝金额。 */
    private String point_amount;

    /** 商户 app_id。 */
    private String app_id;

    /** 买家实付金额。 */
    private String buyer_pay_amount;

    /** 签名类型，如 RSA2。 */
    private String sign_type;

    /** 卖家支付宝用户号。 */
    private String seller_id;

}
