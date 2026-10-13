package order.constant;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 积分抵扣规则：兑换比例与单笔封顶。
 *
 * <p>规则放在 order 侧而不放 member 侧：抵扣上限依赖订单金额（还要先扣掉券优惠），
 * 只有算价的一方能算。member 侧只负责余额的原子增减，不认识"多少积分抵 1 元"这件事。
 *
 * <p>本类不可变、线程安全，只提供静态常量与纯计算方法。
 */
public final class IntegrationConstant {

    /** 兑换比例：100 积分抵 1 元。与"消费 1 元赠 1 积分"配对，相当于 1% 返现。 */
    public static final int POINTS_PER_YUAN = 100;

    /**
     * 单笔抵扣封顶比例：最多抵掉商品应付部分的一半。
     *
     * <p>不是全额可抵：商品金额被积分抵到 0 时平台这一单没有收入，而积分是平台自己发的。
     */
    public static final BigDecimal MAX_DEDUCT_RATIO = new BigDecimal("0.5");

    private IntegrationConstant() {
    }

    /**
     * 把积分数量换算成可抵扣金额。
     *
     * @param points 积分数量，不能为负数
     * @return 抵扣金额，保留 2 位小数；不足 1 分的零头舍去，不四舍五入（多给会员会让抵扣额突破封顶）
     */
    public static BigDecimal pointsToAmount(int points) {
        return new BigDecimal(points).divide(new BigDecimal(POINTS_PER_YUAN), 2, RoundingMode.DOWN);
    }

    /**
     * 算出本单最多能用多少积分。
     *
     * <p>取"封顶上限"与"会员余额"中的较小值。封顶按商品总额减券抵扣后的一半算，<b>不含运费</b>：
     * 运费是付给承运商的成本，用积分抵掉等于平台自己贴。
     *
     * @param goodsAmount 商品总额，不能为 {@code null}
     * @param couponAmount 券抵扣额，不能为 {@code null}；没有用券时传 0
     * @param balance 会员积分余额，不能为负数
     * @return 本单最多可用的积分数，最小为 0；为 0 表示本单用不了积分
     */
    public static int maxUsablePoints(BigDecimal goodsAmount, BigDecimal couponAmount, int balance) {
        BigDecimal deductibleBase = goodsAmount.subtract(couponAmount);
        if (deductibleBase.compareTo(BigDecimal.ZERO) <= 0 || balance <= 0) {
            return 0;
        }

        // 先按封顶算出最大可抵金额、再换算回积分：反过来先算积分数会把不足 1 分的零头算成可用积分，
        // 抵扣额随之后就超过封顶
        BigDecimal maxAmount = deductibleBase.multiply(MAX_DEDUCT_RATIO).setScale(2, RoundingMode.DOWN);
        int maxByRatio = maxAmount.multiply(new BigDecimal(POINTS_PER_YUAN)).intValue();
        return Math.min(maxByRatio, balance);
    }
}
