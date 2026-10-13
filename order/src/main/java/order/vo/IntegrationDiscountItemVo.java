package order.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 积分抵扣摊到某个 SKU 上的金额，是 {@link FareVo#getIntegrationItems()} 与
 * {@link OrderConfirmVo#getIntegrationItems()} 的元素。
 *
 * <p>积分没有适用范围，所以覆盖全部购物项，与券的 {@link CouponDiscountItemVo} 只覆盖券适用范围内的商品不同。
 * 逐项分摊在本服务做：订单项与购物项都在这里，而"按各项金额比例拆分、最后一项吃掉舍入差额"是与商品绑定的算法。
 */
@Data
public class IntegrationDiscountItemVo {

    /** SKU ID。 */
    private Long skuId;

    /** 该 SKU 分到的抵扣额，精确到分。 */
    private BigDecimal discountAmount;

}
