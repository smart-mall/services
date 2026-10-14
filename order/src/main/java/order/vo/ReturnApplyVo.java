package order.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * 会员提交"退货退款"申请的入参。
 *
 * <p>退货是<b>对订单项</b>发起的，不是对订单发起的：一张订单买了三件商品，可以只退其中一件，
 * 所以 {@code orderItemId} 必填，而订单号由服务端按该订单项反查（两个都给会留下"订单项不属于这个订单"
 * 这种要额外校验的组合）。这与评价挂在订单项上是同一个判据：钱要按项退，评价要按项评。
 *
 * <p>{@code reason} 取自原因字典 {@code oms_order_return_reason}（经接口下发，不由前端写死），
 * 它是"收到货之后"的场景，正是退货这一条要表达的。
 */
@Getter
@Setter
public class ReturnApplyVo {

    /** 要退货的订单项主键，必须是该会员自己的、属于已完成订单的订单项。 */
    @NotNull(message = "订单项不能为空")
    private Long orderItemId;

    /** 退货原因，取自原因字典，不能为空，长度不超过 200。 */
    @NotBlank(message = "退货原因不能为空")
    @Size(max = 200, message = "退货原因不能超过 200 字")
    private String reason;

    /** 问题的补充描述，可为空，长度不超过 500。 */
    @Size(max = 500, message = "补充描述不能超过 500 字")
    private String description;

    /** 凭证图片，多个地址用逗号隔开，可为空；长度不超过 2000。 */
    @Size(max = 2000, message = "凭证图片地址过长")
    private String descPics;
}
