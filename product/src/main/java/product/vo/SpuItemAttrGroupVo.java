package product.vo;

import lombok.Data;
import lombok.ToString;

import java.util.List;



/**
 * 商品详情页规格参数中的一个属性分组及其属性值。
 *
 * <p>由 {@code AttrGroupDao#getAttrGroupWithAttrsBySpuId} 的 resultMap 映射，
 * 是 {@link SkuItemVo#getGroupAttrs()} 的元素。
 */
@Data
@ToString
public class SpuItemAttrGroupVo {

    /** 属性分组名，如基本信息、芯片。 */
    private String groupName;

    /** 该分组下当前 spu 的属性名与取值。 */
    private List<Attr> attrs;

}
