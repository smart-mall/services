package product.vo;

import lombok.Data;

/**
 * 一个销售属性取值与拥有该取值的 sku 集合，用于商品详情页的规格选择联动。
 *
 * <p>由 {@code SkuSaleAttrValueDao#getSaleAttrBySpuId} 按属性取值分组聚合，
 * 作为 {@link SkuItemSaleAttrVo#getAttrValues()} 的元素返回，前台据此判断某个取值是否可选。
 */
@Data
public class AttrValueWithSkuIdVo {

    /** 销售属性的一个取值，如「蓝色」。 */
    private String attrValue;

    /** 拥有该取值的全部 sku ID，逗号分隔，由 MySQL {@code group_concat} 聚合得到。 */
    private String skuIds;

}
