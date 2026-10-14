package common.to;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 一个 SKU 的积分设置：购买该 SKU 时可获得的购物积分与成长积分。
 *
 * <p>这是 coupon 与 order、product 之间的契约，按 <b>skuId</b> 索引：调用方（商品详情页、结算页、
 * 下单）手里都只有 skuId，而积分设置本身挂在 SPU 上 —— skuId 到 spuId 的归属由 coupon 侧解析，
 * 免得每个调用方各查一次商品归属。
 *
 * <p><b>本类只承载"规则"，不含"算出来的数量"。</b>基础倍率、按件相乘、向下取整都归
 * {@code common.utils.GiftCalculator}：详情页展示的数与下单实际得到的数必须出自同一份实现。
 */
@Data
public class SkuBoundsVo {

    /** SKU 标识，指向 {@code pms_sku_info.sku_id}。 */
    private Long skuId;

    /** 配置的购物积分值；未配置时为 {@code null}。 */
    private BigDecimal buyBounds;

    /** 配置的成长积分值；未配置时为 {@code null}。 */
    private BigDecimal growBounds;

}
