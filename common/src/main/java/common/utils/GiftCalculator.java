package common.utils;

import common.to.SkuBoundsVo;

import java.math.BigDecimal;

/**
 * 下单赠送积分与成长值的计算规则：按商品原价总额算的基础量，加上商品上配置的加量。
 *
 * <p>放在 {@code common} 是因为两个服务必须用同一份规则：order 在结算页试算"本单可得"、
 * 下单时算快照并交由付款入账；product 在商品详情页展示"买它可得多少"。各写一份必然分叉 ——
 * 页面显示的数与下单实际得到的数一旦对不上，会员看到的就是"说好的没给"。
 *
 * <p>本类不可变、线程安全，只提供静态常量与纯计算方法。
 */
public final class GiftCalculator {

    /**
     * 基础赠送比例：商品原价总额的 10%。
     *
     * <p>这是"下单算好、付款入账"的那份基础量；商品上另可用 {@code sms_spu_bounds} 配一份
     * 固定加量，两者在下单时相加写进订单项快照。没配过积分设置的商品只拿这一份，不额外加量。
     *
     * <p>取整一律向下，与本项目既有的 {@code intValue()} 口径一致。
     */
    public static final BigDecimal BASE_RATE = new BigDecimal("0.1");

    private GiftCalculator() {
    }

    /**
     * 算一份赠送量：基础部分加商品配置部分。
     *
     * <p>购物积分与成长值分开算：基础量对两者相同，配置量分别取 {@code buyBounds} 与
     * {@code growBounds} —— 这两个值本来就可以不一样，用同一个数会抹掉后台的配置。
     *
     * @param originAmount 商品原价总额（单价 × 件数），不能为 {@code null}
     * @param count 购买件数，不能为 {@code null}
     * @param bounds 该商品的积分设置，未配置时为 {@code null}
     * @return 购物积分与成长值两份整数；未配置时两份都只含基础部分
     */
    public static Amount of(BigDecimal originAmount, Integer count, SkuBoundsVo bounds) {
        int base = originAmount.multiply(BASE_RATE).intValue();

        return new Amount(
                base + configured(bounds == null ? null : bounds.getBuyBounds(), count),
                base + configured(bounds == null ? null : bounds.getGrowBounds(), count));
    }

    /**
     * 把商品上配置的积分值换算成整数：配置值 × 件数后向下取整。
     *
     * <p>先乘再取整，而不是先把配置值取整再乘：{@code 150.5 × 2} 应当是 301 而不是 300。
     *
     * @param configured 配置值，可以为 {@code null}（未配置）
     * @param count 购买件数，不能为 {@code null}
     * @return 换算后的整数；未配置时为 0
     */
    private static int configured(BigDecimal configured, Integer count) {
        return configured == null ? 0 : configured.multiply(BigDecimal.valueOf(count)).intValue();
    }

    /**
     * 一份赠送量：购物积分与成长值分开，因为两者的配置来源不同。
     *
     * @param integration 赠送的购物积分
     * @param growth 赠送的成长值
     */
    public record Amount(int integration, int growth) {
    }
}
