package order.vo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 发起支付的请求体。
 *
 * <p>只收支付方式：订单号在路径里，金额和收款方由后端按订单查，不接受前端传 ——
 * 否则改一个数字就能把应付金额改成任意值。</p>
 */
@Data
public class PayRequestVo {

    /** 1 支付宝 / 2 微信，取值见 {@link order.constant.PayConstant}。 */
    @NotNull(message = "不能为空")
    private Integer payType;

}
