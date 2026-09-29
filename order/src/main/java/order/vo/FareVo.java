package order.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 结算金额，面向前端：商品总额、运费、优惠与应付四个金额，以及运费与优惠各自按商品的明细。
 *
 * <p>四个金额都由服务端算定，前端只负责显示并原样回传 {@code payAmount}。
 * 前端若自己按「商品总额 + 运费 − 优惠」再算一遍，服务端一改加价规则就会算出两个数，
 * 提交时被判成价格变动。
 *
 * <p>{@code fareItems} 与 {@code couponItems} 都按 {@code skuId} 与购物项对应，但两者覆盖的商品
 * 不一定相同：运费覆盖全部购物项，优惠只覆盖券适用范围内的那些，范围外的商品不会出现在后者里。
 */
@Data
public class FareVo {

    /** 商品总额，不含运费，也不减优惠。 */
    private BigDecimal totalAmount;

    /** 整单运费。 */
    private BigDecimal freightAmount;

    /** 整单优惠金额；没有用券时为 0，不会为 {@code null}。 */
    private BigDecimal couponAmount;

    /** 应付总额 = 商品总额 + 运费 − 优惠金额。 */
    private BigDecimal payAmount;

    /** 每个商品的运费明细，前端按它逐行展示。 */
    private List<FareItemVo> fareItems;

    /**
     * 每个商品的优惠明细，与 {@code fareItems} 同一形状，前端按它逐行展示优惠。
     *
     * <p>没有用券时为空列表，不会为 {@code null}；只含券适用范围内的商品。
     */
    private List<CouponDiscountItemVo> couponItems;
}
