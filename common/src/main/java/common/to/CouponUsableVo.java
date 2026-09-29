package common.to;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * 一张可用于给定购物车的券，由 coupon 组装后传给 order，order 再并入结算页数据。
 *
 * <p>两个总额是配对的：{@code scopeAmount} 是券适用范围内的商品金额（门槛比的就是它），
 * {@code discountAmount} 是实际抵扣额（券面金额与 {@code scopeAmount} 中的较小者，
 * 不会把订单减成负数）。调用方不要自己拿券面金额去减 —— 范围外商品占多数时会算多。
 *
 * <p>抵扣额按商品怎么摊不在这里：coupon 只声明范围（{@code scopeSkuIds}），
 * 逐项分摊由 order 做，因为购物项与订单项都在它手里。
 */
@Data
public class CouponUsableVo {

    /** 领取记录 ID，下单时回传它来指定用哪张券。 */
    private Long couponHistoryId;

    /** 券模板 ID。 */
    private Long couponId;

    /** 券名称。 */
    private String couponName;

    /** 券面金额。 */
    private BigDecimal amount;

    /** 使用门槛。 */
    private BigDecimal minPoint;

    /** 券的可使用结束时间。 */
    private Date couponEndTime;

    /** 券适用范围内的商品金额，门槛比的就是它。 */
    private BigDecimal scopeAmount;

    /** 本单实际抵扣金额。 */
    private BigDecimal discountAmount;

    /**
     * 抵扣额覆盖的 SKU 集合，供调用方逐项分摊。
     *
     * <p>全场券就是购物车里的全部 SKU；指定范围的券只含命中的那些，范围外的商品不在其中。
     */
    private List<Long> scopeSkuIds;

}
