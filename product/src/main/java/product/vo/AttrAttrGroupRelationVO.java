package product.vo;

import lombok.Data;

/**
 * 属性与属性分组的绑定关系入参，携带一对 {@code attrId} + {@code attrGroupId}。
 *
 * <p>当前绑定与解绑接口使用的是 {@link AttrGroupRelationVO}，本类无调用方。
 */
@Data
public class AttrAttrGroupRelationVO {

    // 批量绑定的请求体形如 [{"attrId":1,"attrGroupId":2}]，每项是一对属性与分组 ID

    /** 属性 ID，指向 {@code pms_attr.attr_id}。 */
    private Long attrId;

    /** 属性分组 ID，指向 {@code pms_attr_group.attr_group_id}。 */
    private Long attrGroupId;
}
