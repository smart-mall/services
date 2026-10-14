package common.to;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 某个 SPU 的积分设置：购买该 SPU 时可获得的购物积分与成长积分。
 *
 * <p>这是 coupon 与 order 之间的契约：积分设置存在 coupon 库的 {@code sms_spu_bounds} 表里，
 * 而下单时算赠送量的是 order，order 不去读别人的表。放在 {@code common/to} 的理由与
 * {@link MemberLevelVo} 相同 —— 两个服务各存一份副本时，一边加字段、另一边没加不会编译报错，
 * Jackson 只会把缺的字段读成 {@code null}。
 *
 * <p><b>本类只承载"规则"，不含"算出来的数量"。</b>配置值按件相乘、向下取整、以及与基础赠送量
 * 怎么相加，全部由调用方（order）决定：规则归规则持有方，算钱归算价的一方，与券那一轮同一条边界。
 */
@Data
public class SpuBoundsVo {

    /** SPU 标识，指向 {@code pms_spu_info.id}。 */
    private Long spuId;

    /**
     * 配置的购物积分值，对应 {@code sms_spu_bounds.buy_bounds}。
     *
     * <p>落进订单项的是它与购买件数相乘并向下取整后的整数；未配置时为 {@code null}。
     */
    private BigDecimal buyBounds;

    /**
     * 配置的成长积分值，对应 {@code sms_spu_bounds.grow_bounds}。
     *
     * <p>落进订单项的是它与购买件数相乘并向下取整后的整数；未配置时为 {@code null}。
     */
    private BigDecimal growBounds;

}
