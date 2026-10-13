package order.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 修改订单备注的请求体（后台管理端）。
 *
 * <p>可改字段只有备注一个：订单状态与金额各有专属的业务动作与状态流转，
 * 不通过一个能写任意列的口子放开。</p>
 */
@Data
public class OrderNoteVo {

    /** 订单号，业务主键，非自增 ID。 */
    @NotBlank(message = "不能为空")
    private String orderSn;

    /** 订单备注；允许为空串或 {@code null}，表示清空。长度上限与 {@code oms_order.note} 列一致。 */
    @Size(max = 500, message = "长度不能超过 500")
    private String note;

}
