package product.vo;

import lombok.Data;

import java.util.List;

/**
 * 商品详情页规格参数中的一个属性分组及其组内属性。
 *
 * <p>当前前台商品详情页的规格参数由 {@link SpuItemAttrGroupVo} 承载，本类无调用方。
 */
@Data
public class ItemAttrGroupWithAttrVO {

    /** 属性分组 ID，指向 {@code pms_attr_group.attr_group_id}。 */
    private Long attrGroupId;

    /** 属性分组名，如基本信息、芯片。 */
    private String attrGroupName;

    /** 该分组下当前 spu 的属性名与取值。 */
    private List<ItemAttrVO> attrs;
}
