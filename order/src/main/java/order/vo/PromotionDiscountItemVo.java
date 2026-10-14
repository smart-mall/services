package order.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 一个 SKU 命中的商品促销减免额，是 {@link FareVo#getPromotionItems()} 与
 * {@link OrderConfirmVo#getPromotionItems()} 的元素。
 *
 * <p>命中判定在本服务做，券服务只声明该 SKU 配了哪些规则：满减按行金额判、阶梯价按件数判，
 * 而"这一行是多少钱、买了几件"只有算价的一方知道。
 */
@Data
public class PromotionDiscountItemVo {

    /** SKU ID。 */
    private Long skuId;

    /** 该 SKU 的促销减免额，精确到分。 */
    private BigDecimal discountAmount;

}
