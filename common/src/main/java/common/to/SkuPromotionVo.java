package common.to;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 一个 SKU 的商品优惠规则：满减、阶梯价与会员价。
 *
 * <p>这是 coupon 与 order（以及 product）之间的契约：规则存在 coupon 库的
 * {@code sms_sku_full_reduction} 与 {@code sms_sku_ladder} 里，而算进金额的是 order，
 * order 不去读别人的表。放在 {@code common/to} 的理由与 {@link MemberLevelVo} 相同。
 *
 * <p><b>本类只承载"规则"，不含"算出来的减免额"。</b>命中哪一档、折后单价怎么算、
 * 与满减怎么先后作用，全部由调用方（order）决定 —— 规则归规则持有方，算钱归算价的一方，
 * 与券那一轮同一条边界。
 *
 * <p>没有配置的那一段，其字段全为 {@code null}（只有满减就阶梯三个字段为 {@code null}，
 * 反之亦然），调用方据此判断该段是否存在，不要用 0 去猜。
 */
@Data
public class SkuPromotionVo {

    /** SKU 标识，指向 {@code pms_sku_info.sku_id}。 */
    private Long skuId;

    /** 满减：满多少元（按本行金额判定）。 */
    private BigDecimal fullPrice;

    /** 满减：减多少元。 */
    private BigDecimal reducePrice;

    /** 阶梯价：满几件。 */
    private Integer fullCount;

    /** 阶梯价：打几折，如 {@code 0.75} 表示七五折。 */
    private BigDecimal discount;

    /**
     * 阶梯价：折后单价。
     *
     * <p>与 {@link #discount} 同时有值时以本字段为准：它是后台明确填的折后价，
     * 比"按折扣反算"更具体，也不会因为四舍五入与原价对不上。
     */
    private BigDecimal ladderPrice;

    /**
     * 指定等级的会员价，对应 {@code sms_member_price.member_price}。
     *
     * <p>只有调用方传了 {@code levelId}、且该 SKU 配了这个等级的价格时才有值，其余为 {@code null}。
     * 它是"这个等级能拿到的价"，命中后按单价优惠处理。
     */
    private BigDecimal memberPrice;

    /**
     * 该 SKU 所有等级里最低的会员价，供商品详情页展示"会员价 ¥X 起"。
     *
     * <p>与 {@link #memberPrice} 的区别是它**不需要身份**：详情页是公开接口、拿不到会员的等级，
     * 但会员价配置本身是公开的。没配过任何等级的价格时为 {@code null}。
     */
    private BigDecimal memberPriceFrom;

}
