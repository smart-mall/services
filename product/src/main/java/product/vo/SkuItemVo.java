package product.vo;


import common.to.SkuPromotionVo;
import lombok.Data;
import lombok.ToString;
import product.entity.SkuImagesEntity;
import product.entity.SkuInfoEntity;
import product.entity.SpuInfoDescEntity;

import java.math.BigDecimal;
import java.util.List;



/**
 * 商品详情页的完整出参：sku 基本信息、库存、图集、销售属性、商品介绍、规格参数、秒杀优惠、商品优惠
 * 规则、按件试算的赠送量与最低会员价。
 *
 * <p>由 {@code SkuInfoServiceImpl#item} 并行组装，是 {@code /product/front/item/{skuId}} 的出参；
 * 除 {@code info} 外的各数据块都可能为 {@code null}（远程服务失败或数据未维护）。
 */
@ToString
@Data
public class SkuItemVo {

    /** sku 基本信息，来自 {@code pms_sku_info}；sku 不存在时为 {@code null}。 */
    private SkuInfoEntity info;

    /** 该 sku 是否有货；默认 true，库存服务查询失败时保留默认值。 */
    private boolean hasStock = true;

    /** 该 sku 的图集，来自 {@code pms_sku_images}。 */
    private List<SkuImagesEntity> images;

    /** 同款商品（同 spu）的销售属性组合，用于详情页选择规格。 */
    private List<SkuItemSaleAttrVo> saleAttr;

    /** 商品介绍（描述图片），来自 {@code pms_spu_info_desc}。 */
    private SpuInfoDescEntity desc;

    /** 商品规格参数，按属性分组组织。 */
    private List<SpuItemAttrGroupVo> groupAttrs;

    /** 秒杀优惠信息；该 sku 不参与秒杀或秒杀已结束时为 {@code null}。 */
    private SeckillSkuVo seckillSkuVo;

    /** 该 sku 的商品优惠规则（满减、阶梯价与会员价）；都没配时为 {@code null}。 */
    private SkuPromotionVo promotion;

    /**
     * 按 1 件试算的赠送购物积分；没配过积分设置时为"只有基础量"的那一份，不会为 {@code null}。
     *
     * <p>详情页只显示"每件预计可得"，本单的准确数额由结算页下发：按件数相乘后取整的结果
     * 不是线性的（{@code 19.9 × 1} 与 {@code 19.9 × 2} 取整后不成倍数），前端自己乘会差几分。
     */
    private Integer unitGiftIntegration;

    /** 按 1 件试算的赠送成长值，口径同 {@link #unitGiftIntegration}。 */
    private Integer unitGiftGrowth;

    /**
     * 该 sku 所有等级里最低的会员价，供详情页展示"会员价 ¥X 起"；没配过会员价时为 {@code null}。
     *
     * <p>它不需要身份：会员价配置本身是公开的，谁都能看到"最低能到多少"；具体按哪个等级成交
     * 要到结算页才知道（那里有会员凭证）。
     */
    private BigDecimal memberPriceFrom;

}
