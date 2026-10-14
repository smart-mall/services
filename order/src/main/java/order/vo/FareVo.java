package order.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 结算金额，面向前端：商品总额、运费、商品优惠、券优惠、积分抵扣与应付六个金额、本单可得的赠送量，
 * 以及四份按商品的明细。
 *
 * <p>六个金额都由服务端算定，前端只负责显示并原样回传 {@code payAmount}。
 * 前端若自己按「商品总额 + 运费 − 促销优惠 − 券优惠 − 积分抵扣」再算一遍，服务端一改加价规则就会算出两个数，
 * 提交时被判成价格变动。
 *
 * <p>{@code payAmount = totalAmount + freightAmount − promotionAmount − couponAmount − integrationAmount}。
 *
 * <p>四份明细都按 {@code skuId} 与购物项对应，覆盖范围不同：{@code fareItems} 与
 * {@code integrationItems} 覆盖全部购物项，{@code promotionItems} 与 {@code couponItems}
 * 只覆盖命中的那些商品，没命中的不会出现在后者里。
 */
@Data
public class FareVo {

    /** 商品总额，不含运费，也不减优惠。 */
    private BigDecimal totalAmount;

    /** 整单运费。 */
    private BigDecimal freightAmount;

    /**
     * 整单运费是否被会员的免邮权益抵掉；为 {@code true} 时 {@link #freightAmount} 必为 0。
     *
     * <p>只用于结算页标注"会员免邮"。{@code freightAmount} 为 0 有两种来源 —— 会员免邮，或计费结果
     * 本来就是 0 —— 前端凭金额分不出来，标注只能靠这个字段。
     */
    private boolean memberFreeFreight;

    /** 整单商品优惠减免（满减、阶梯价与会员价）；一项都没命中时为 0，不会为 {@code null}。 */
    private BigDecimal promotionAmount;

    /** 整单优惠金额；没有用券时为 0，不会为 {@code null}。 */
    private BigDecimal couponAmount;

    /** 整单积分抵扣金额；没有用积分时为 0，不会为 {@code null}。 */
    private BigDecimal integrationAmount;

    /** 应付总额 = 商品总额 + 运费 − 促销优惠 − 券优惠 − 积分抵扣。 */
    private BigDecimal payAmount;

    /**
     * 本单最多可用的积分数，取封顶上限与余额中的较小值。
     *
     * <p>封顶按"商品总额减券抵扣"算，所以换券之后这个数会变 —— 重算接口必须把它一起带回来，
     * 否则前端会拿换券前的旧上限去限定输入范围，选出来的数量提交时必被拒。
     * 为 0 表示本单用不了积分。
     */
    private Integer integrationLimit;

    /** 每个商品的运费明细，前端按它逐行展示。 */
    private List<FareItemVo> fareItems;

    /**
     * 每个商品的商品优惠明细，与 {@code fareItems} 同一形状，前端按它逐行展示。
     *
     * <p>没有命中时为空列表，不会为 {@code null}；只含命中的商品（满减、阶梯价或会员价任一命中）。
     */
    private List<PromotionDiscountItemVo> promotionItems;

    /**
     * 每个商品的优惠明细，与 {@code fareItems} 同一形状，前端按它逐行展示优惠。
     *
     * <p>没有用券时为空列表，不会为 {@code null}；只含券适用范围内的商品。
     */
    private List<CouponDiscountItemVo> couponItems;

    /**
     * 每个商品的积分抵扣明细，与 {@code fareItems} 同一形状，前端按它逐行展示积分抵扣。
     *
     * <p>没有用积分时为空列表，不会为 {@code null}；积分没有适用范围，所以覆盖全部购物项。
     */
    private List<IntegrationDiscountItemVo> integrationItems;

    /**
     * 商品优惠里来自会员价的那部分；没有会员价特权或没配过会员价时为 0。
     *
     * <p>只给前端拆分展示用：{@code promotionAmount} 是含会员价的合计（与订单项上的
     * {@code promotion_amount} 一致），本字段是其中属于会员价的那一块。
     */
    private BigDecimal memberPriceAmount;

    /**
     * 本单可得的赠送积分，与下单入账用的是同一份算法（{@code common.utils.GiftCalculator}）。
     *
     * <p>只用于展示：真正的数额在下单那一刻按订单项算定并写进快照，付款时才入账。
     */
    private Integer giftIntegration;

    /** 本单可得的成长值，口径同 {@link #giftIntegration}。 */
    private Integer giftGrowth;
}
