package order.vo;

import lombok.Data;

/**
 * 订单状态投影：ware 的内部调用只要 {@code status}，SPA 的状态轮询要 {@code statusText}。
 * 刻意不返回整单 —— 内部那条路径（{@code /order/order/status/{orderSn}}）免登录，返回整单会暴露收货人信息。
 */
@Data
public class OrderStatusVo {

    /** 订单号，业务主键，非自增 ID。 */
    private String orderSn;

    /** 取值见 {@link order.enume.OrderStatusEnum}。 */
    private Integer status;

    /** 状态文案，如"待付款"。 */
    private String statusText;

}
