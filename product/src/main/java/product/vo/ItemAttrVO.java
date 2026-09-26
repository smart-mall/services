package product.vo;

import lombok.Data;

/**
 * 商品详情页规格参数（基本属性）中的一条，属性值为单个值。
 *
 * <p>当前前台商品详情页的规格参数由 {@link SpuItemAttrGroupVo} 与 {@link Attr} 承载，本类无调用方。
 */
@Data
public class ItemAttrVO {

    /** 属性 ID，指向 {@code pms_attr.attr_id}。 */
    private Long attrId;
    /** 属性名。 */
    private String attrName;
    /** 当前 spu 在该属性上的取值；规格参数（基本属性）为单值，不是逗号分隔的列表。 */
    private String attrValue;
}
