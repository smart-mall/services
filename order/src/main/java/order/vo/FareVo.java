package order.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 结算金额，面向前端：商品总额、运费、券优惠、积分抵扣与应付五个金额，以及三份按商品的明细。
 *
 * <p>五个金额都由服务端算定，前端只负责显示并原样回传 {@code payAmount}。
 * 前端若自己按「商品总额 + 运费 − 券优惠 − 积分抵扣」再算一遍，服务端一改加价规则就会算出两个数，
 * 提交时被判成价格变动。
 *
 * <p>{@code payAmount = totalAmount + freightAmount − couponAmount − integrationAmount}。
 *
 * <p>三份明细都按 {@code skuId} 与购物项对应，覆盖范围不同：{@code fareItems} 与
 * {@code integrationItems} 覆盖全部购物项，{@code couponItems} 只覆盖券适用范围内的那些，
 * 范围外的商品不会出现在后者里。
 */
@Data
public class FareVo {

    /** 商品总额，不含运费，也不减优惠。 */
    private BigDecimal totalAmount;

    /** 整单运费。 */
    private BigDecimal freightAmount;

    /** 整单优惠金额；没有用券时为 0，不会为 {@code null}。 */
    private BigDecimal couponAmount;

    /** 整单积分抵扣金额；没有用积分时为 0，不会为 {@code null}。 */
    private BigDecimal integrationAmount;

    /** 应付总额 = 商品总额 + 运费 − 券优惠 − 积分抵扣。 */
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
}
