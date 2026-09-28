package ware.vo;

import lombok.Data;

import java.util.List;

/**
 * 锁定库存的入参，对应 {@code POST /ware/waresku/lock/order}：order 侧下单时按订单号锁住一批购物项。
 *
 * <p>除订单号外还带一组订单快照（订单 ID、收货人、配送地址、备注、付款方式），
 * 用来把库存工作单填成自包含的记录，发货时不必回查订单。
 */

@Data
public class WareSkuLockVo {

    /** 订单 ID，写入工作单的 {@code order_id}。 */
    private Long orderId;

    /** 订单号，ware 侧按它建库存工作单，后续解锁也用它找回锁定明细。 */
    private String orderSn;

    /** 收货人姓名。 */
    private String consignee;

    /** 收货人电话。 */
    private String consigneeTel;

    /** 配送地址，省市区与详细地址拼成一行。 */
    private String deliveryAddress;

    /** 订单备注，即用户下单时填的备注。 */
    private String orderComment;

    /** 付款方式：1 在线付款，2 货到付款。 */
    private Integer paymentWay;

    /** 需要锁定库存的购物项，每个 SKU 只锁一个有货的仓库；任一 SKU 锁不上就整单回滚。 */
    private List<OrderItemVo> locks;



}
