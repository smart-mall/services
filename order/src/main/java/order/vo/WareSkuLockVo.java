package order.vo;

import lombok.Data;

import java.util.List;

/**
 * 锁定库存的入参：按订单号锁住一批购物项，并把仓库建工作单要用的订单信息一并带过去。
 *
 * <p>收货信息取的是下单那一刻的快照。带上它们是为了让 ware 侧建出的库存工作单自包含，
 * 发货流程不必再回查订单。
 */

@Data
public class WareSkuLockVo {

    /** 订单 ID，ware 侧写入工作单的 {@code order_id}。 */
    private Long orderId;

    /** 订单号，ware 侧按它记录锁定关系，后续解锁也用这个号。 */
    private String orderSn;

    /** 收货人姓名。 */
    private String consignee;

    /** 收货人电话。 */
    private String consigneeTel;

    /** 配送地址，省市区与详细地址拼成一行。 */
    private String deliveryAddress;

    /** 订单备注，即用户下单时填的备注。 */
    private String orderComment;

    /**
     * 付款方式：1 在线付款，2 货到付款。
     *
     * <p>与 {@code oms_order.pay_type} 取值口径不同，由 order 侧换算后传入。
     */
    private Integer paymentWay;

    /** 需要锁住库存的购物项。 */
    private List<OrderItemVo> locks;

}
