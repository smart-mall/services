package product.vo;


import lombok.Data;
import product.entity.SkuImagesEntity;
import product.entity.SkuInfoEntity;
import product.entity.SpuInfoDescEntity;

import java.util.List;

/**
 * 商品详情页数据模型：聚合 sku 基本信息、库存、图集、销售属性、商品介绍与规格参数。
 *
 * <p>当前前台商品详情接口 {@code /product/front/item/{skuId}} 返回的是 {@link SkuItemVo}，本类无调用方。
 */
@Data
public class ItemDetailVO {

    /** sku 基本信息；sku 不存在时为 {@code null}。 */
    private SkuInfoEntity skuInfo;

    /** 该 sku 是否有货；默认 false。 */
    private boolean hasStock = false;

    /** 该 sku 的图集。 */
    private List<SkuImagesEntity> skuImages;

    /** 同款商品（同 spu）的全部销售属性组合。 */
    private List<ItemSaleAttrVO> saleAttrs;

    /** 商品介绍（描述图片），来自 {@code pms_spu_info_desc}。 */
    private SpuInfoDescEntity spuDesc;

    /** 商品规格参数，按属性分组组织。 */
    private List<ItemAttrGroupWithAttrVO> spuAttrGroups;

}
