package order.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * 会员提交"仅退款"申请的入参。
 *
 * <p><b>只有订单号与一段说明，没有原因下拉、也没有凭证图片</b>，两处都是刻意的：
 * <ul>
 *   <li>项目里的原因字典 {@code oms_order_return_reason} 取值全是"收到货之后"的场景
 *       （质量问题、商品与描述不符、七天无理由、收到破损），套到"还没发货就不想要了"上不成立，
 *       所以退款这一条不填 {@code reason}，把话写在说明里 —— 与"来源没有实现就留 NULL、不填假值"同一口径。
 *       硬套一张语义不合的字典，等于让报表里出现一批解释不通的原因。</li>
 *   <li>货还没发出去，会员手上没有可拍的东西，凭证图片没有来源。</li>
 * </ul>
 */
@Getter
@Setter
public class RefundApplyVo {

    /** 要退款的订单号，必须是该会员自己的、已付款且未发货的订单。 */
    @NotBlank(message = "订单号不能为空")
    private String orderSn;

    /** 申请说明，写清楚为什么不要了；不能为空，长度不超过 500。 */
    @NotBlank(message = "申请说明不能为空")
    @Size(max = 500, message = "申请说明不能超过 500 字")
    private String description;
}
