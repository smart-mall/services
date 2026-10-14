package order.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 状态选项，供管理端状态筛选的下拉框使用，订单状态与售后申请状态共用。
 *
 * <p>它就是一个 {@code code + text} 的选项对，两种状态共用同一个形状而不是各写一份。
 * 文案由服务端下发：{@link order.enums.OrderStatusEnum} 与 {@link order.enums.AfterSaleStatusEnum}
 * 是各自文案的唯一来源，前端再维护一份就多了一个会漂移的副本。</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatusOptionVo {

    /** 状态码，筛选时原样回传给服务端。 */
    private Integer code;

    /** 状态文案，下拉框里显示。 */
    private String text;

}
