package order.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * 模拟支付平台对退款请求的应答。
 *
 * <p>形状取两家的并集，两个字段各自都有真实出处：
 * <ul>
 *   <li>{@code code} / {@code fund_change} —— 来自支付宝。{@code code = 10000} <b>只表示接口调用成功</b>，
 *       要知道钱有没有动必须看 {@code fund_change}：{@code Y} 表示资金确实变了，{@code N} 表示没变
 *       （典型场景是这笔已经退过）。</li>
 *   <li>{@code refund_id} —— 平台侧的退款单号，来自微信 V3（支付宝的退款没有独立单号）。
 *       项目把它记进 {@code oms_refund_info.refund_sn}，好让退款流水有一个平台侧的凭证号。</li>
 * </ul>
 *
 * <p>它是第三方平台的报文，形状不受本项目 {@code R<T>} 的约束，所以 {@code code} 是字符串而不是数字。
 */
@Getter
@Setter
public class MockRefundResultVo {

    /** 平台返回码，{@code 10000} 表示接口调用成功。 */
    private String code;

    /** 平台返回的说明，失败时供排查。 */
    private String msg;

    /** 资金是否发生了变动，{@code Y} 为退款成功、{@code N} 为没有变动。 */
    @JsonProperty("fund_change")
    private String fundChange;

    /** 平台侧的退款单号，记进退款流水。 */
    @JsonProperty("refund_id")
    private String refundId;

    /**
     * 判断这次退款是否真的成功了。
     *
     * <p>两个条件缺一不可：{@code code} 为 {@code 10000}（接口调用成功）且 {@code fund_change}
     * 为 {@code Y}（资金确实变了）。只看 {@code code} 会把"重复退款、钱没动"当成退款成功。</p>
     *
     * @return 两个条件都满足时为 {@code true}
     */
    public boolean isFundChanged() {
        return "10000".equals(code) && "Y".equalsIgnoreCase(fundChange);
    }
}
