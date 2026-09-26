package product.vo;

import lombok.Data;
import product.entity.AttrEntity;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 属性分组及其关联属性，用于按三级分类查询分组配置。
 *
 * <p>当前按分类查询分组配置的接口使用的是 {@link AttrGroupWithAttrsVO}，本类无调用方。
 */

@Data
public class AttrGroupWithAttrVO implements Serializable {

    @Serial private static final long serialVersionUID = 1L;

    /** 分组 ID，指向 {@code pms_attr_group.attr_group_id}。 */
    private Long attrGroupId;
    /** 分组名，如基本信息、芯片。 */
    private String attrGroupName;
    /** 同一分类下分组的展示顺序。 */
    private Integer sort;
    /** 分组描述。 */
    private String descript;
    /** 分组图标地址。 */
    private String icon;
    /** 所属三级分类 ID，指向 {@code pms_category.cat_id}。 */
    private Long catalogId;

    /** 该分组关联的全部属性。 */
    private List<AttrEntity> attrs;
}
