package product.vo;

import lombok.Data;
import lombok.ToString;

import java.util.List;


/**
 * 商品详情页的一个销售属性及其全部取值，每个取值带上拥有该取值的 sku。
 *
 * <p>由 {@code SkuSaleAttrValueDao#getSaleAttrBySpuId} 的 resultMap 映射，
 * 是 {@link SkuItemVo#getSaleAttr()} 的元素，供前台按属性取值组合定位到具体 sku。
 */
@Data
@ToString
public class SkuItemSaleAttrVo {

    /** 属性 ID，指向 {@code pms_attr.attr_id}。 */
    private Long attrId;

    /** 属性名，如颜色、内存。 */
    private String attrName;

    /** 该属性的全部取值，每个取值附带拥有它的 sku ID 列表。 */
    private List<AttrValueWithSkuIdVo> attrValues;

}
