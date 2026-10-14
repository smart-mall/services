package order.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 一个 SKU 命中的商品优惠减免额，是 {@link FareVo#getPromotionItems()} 与
 * {@link OrderConfirmVo#getPromotionItems()} 的元素。
 *
 * <p>命中判定在本服务做，券服务只声明该 SKU 配了哪些规则：满减按行金额判、阶梯价按件数判、
 * 会员价按会员的等级取，而"这一行是多少钱、买了几件、这个会员是什么等级"只有算价的一方知道。
 */
@Data
public class PromotionDiscountItemVo {

    /** SKU ID。 */
    private Long skuId;

    /** 该 SKU 的商品优惠减免额，精确到分。 */
    private BigDecimal discountAmount;

    /**
     * 其中来自会员价的部分；会员价没生效（没配、没特权、或被阶梯价压得更低）时为 0。
     *
     * <p>只给展示层拆分用：订单项上只有一个 {@code promotion_amount}，表结构不动。
     */
    private BigDecimal memberPriceAmount;

}
