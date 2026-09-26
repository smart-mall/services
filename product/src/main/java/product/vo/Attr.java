/** Copyright 2020 bejson.com */
package product.vo;

import lombok.Data;

/**
 * 商品详情页规格参数中的单条属性：属性名与当前 spu 在该属性上的取值。
 *
 * <p>由 {@code AttrGroupDao#getAttrGroupWithAttrsBySpuId} 的 resultMap 直接映射，
 * 作为 {@link SpuItemAttrGroupVo#getAttrs()} 的元素返回给前台商品详情页。
 */
@Data
public class Attr {

  /** 属性 ID，指向 {@code pms_attr.attr_id}。 */
  private Long attrId;
  /** 属性名，如品牌、屏幕尺寸。 */
  private String attrName;
  /** 当前 spu 在该属性上的取值；规格参数（基本属性）为单值。 */
  private String attrValue;
}
