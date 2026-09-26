package order.vo;

import lombok.Data;

import java.util.List;

/**
 * 锁定库存的入参：按订单号锁住一批购物项。
 */

@Data
public class WareSkuLockVo {

    /** 订单号，ware 侧按它记录锁定关系，后续解锁也用这个号。 */
    private String orderSn;

    /** 需要锁住库存的购物项。 */
    private List<OrderItemVo> locks;

}
