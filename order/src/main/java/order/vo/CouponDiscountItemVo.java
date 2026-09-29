package order.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 一张券摊到某个 SKU 上的抵扣额，是 {@link FareVo#getCouponItems()} 与
 * {@link OrderConfirmVo#getCouponItems()} 的元素。
 *
 * <p>分摊在本服务做，券服务只声明抵扣覆盖了哪些 SKU：购物项与订单项都在本服务手里，
 * 而"按各项金额比例拆分、最后一项吃掉舍入差额"是与商品绑定的算法。
 */
@Data
public class CouponDiscountItemVo {

    /** SKU ID。 */
    private Long skuId;

    /** 该 SKU 分到的抵扣额，精确到分。 */
    private BigDecimal discountAmount;

}
