package product.vo;

import lombok.Data;

import java.util.List;

/**
 * 商品详情页的一个销售属性及其全部取值，每个取值带上拥有该取值的 sku。
 *
 * <p>结构相近的 {@link SkuItemSaleAttrVo} 才是当前前台商品详情页使用的出参，本类无调用方。
 */
@Data
public class ItemSaleAttrVO {

    /** 属性 ID，指向 {@code pms_attr.attr_id}。 */
    private Long attrId;

    /** 属性名，如颜色、内存。 */
    private String attrName;

    /** 该属性的全部取值，每个取值附带拥有它的 sku ID 列表。 */
    List<ItemSaleAttrValueWithSkuVO> attrValueWithSkus;

}


