package ware.vo;

import lombok.Data;

import java.util.List;

/**
 * 锁定库存的入参，对应 {@code POST /ware/waresku/lock/order}：order 侧下单时按订单号锁住一批购物项。
 */

@Data
public class WareSkuLockVo {

    /** 订单号，ware 侧按它建库存工作单，后续解锁也用它找回锁定明细。 */
    private String orderSn;

    /** 需要锁定库存的购物项，每个 SKU 只锁一个有货的仓库；任一 SKU 锁不上就整单回滚。 */
    private List<OrderItemVo> locks;



}
