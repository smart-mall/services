package product.vo;


import lombok.Data;
import lombok.ToString;
import product.entity.SkuImagesEntity;
import product.entity.SkuInfoEntity;
import product.entity.SpuInfoDescEntity;

import java.util.List;



/**
 * 商品详情页的完整出参：sku 基本信息、库存、图集、销售属性、商品介绍、规格参数与秒杀优惠。
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

}
