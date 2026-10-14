package order.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * 向模拟支付平台发起退款时的请求体。
 *
 * <p>字段名与真实渠道对齐（这里取支付宝的措辞），因为这个请求将来指向真实平台时，
 * 换的应当只有地址、证书与签名，而不是业务参数的名字：
 * <ul>
 *   <li>{@code out_request_no} —— <b>商户退款单号</b>，退款链路上唯一的幂等锚。同一个单号重复提交，
 *       平台返回同一笔结果、不会重复退钱。</li>
 *   <li>{@code refund_amount} —— 退款金额，字符串形式保留两位小数（真实渠道也要求字符串，
 *       用浮点传会在序列化时丢精度）。</li>
 *   <li>{@code trade_no} —— 原支付交易号，平台据此定位要退哪笔钱。</li>
 * </ul>
 */
@Getter
@Setter
public class MockRefundRequestVo {

    /** 商户退款单号，退款链路的幂等锚，不能为 {@code null}。 */
    @JsonProperty("out_request_no")
    private String outRequestNo;

    /** 退款金额，两位小数的字符串，不能为 {@code null}。 */
    @JsonProperty("refund_amount")
    private String refundAmount;

    /** 原支付交易号，不能为 {@code null}。 */
    @JsonProperty("trade_no")
    private String tradeNo;
}
