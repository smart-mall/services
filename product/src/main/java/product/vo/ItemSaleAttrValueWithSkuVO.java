package product.vo;

import lombok.Data;

/**
 * 商品详情页中一个销售属性取值与拥有该取值的 sku 集合。
 *
 * <p>用于把用户选中的属性取值组合映射到具体 sku：例如颜色取值「绿色」与内存取值「8G」都关联了
 * 1 号 sku，选中这两个取值就能确定商品是 1 号。
 */
@Data
public class ItemSaleAttrValueWithSkuVO {

    /** 销售属性的一个取值，如「绿色」。 */
    String attrValue;
    /** 拥有该取值的全部 sku ID，逗号分隔。 */
    String skuIds;
}
