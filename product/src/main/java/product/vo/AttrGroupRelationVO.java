package product.vo;

import lombok.Data;

/**
 * 属性与属性分组的绑定关系入参，携带一对 {@code attrId} + {@code attrGroupId}。
 *
 * <p>用于属性分组的批量绑定与解绑接口（{@code /product/attrgroup/attr/relation}），
 * 解绑时按这两个字段定位关联行。
 */
@Data
public class AttrGroupRelationVO {
    /** 属性 ID，指向 {@code pms_attr.attr_id}。 */
    private Long attrId;
    /** 属性分组 ID，指向 {@code pms_attr_group.attr_group_id}。 */
    private Long attrGroupId;
}
