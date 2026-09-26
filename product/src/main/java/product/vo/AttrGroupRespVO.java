package product.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;
import product.entity.AttrGroupEntity;

/**
 * 属性分组列表出参，在 {@link AttrGroupEntity} 上补充分组所属分类名。
 *
 * <p>由 {@code AttrGroupServiceImpl#queryPage} 组装，是
 * {@code /product/attrgroup/list/{categoryId}} 的出参，供后台属性分组列表页展示。
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class AttrGroupRespVO extends AttrGroupEntity {
    /** 所属分类名，由分组上的 {@code catalogId} 回查 {@code pms_category} 得到。 */
    private String catalogName;
}
