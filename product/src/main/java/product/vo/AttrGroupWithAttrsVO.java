package product.vo;

import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;
import product.entity.AttrEntity;

import java.util.List;

/**
 * 属性分组及其已绑定的属性，用于按三级分类查询分组配置。
 *
 * <p>由 {@code AttrGroupServiceImpl#getAttrGroupWithAttrs} 组装，是
 * {@code /product/attrgroup/{catalogId}/withattr} 的出参，供后台维护分组与属性的绑定关系。
 */
@Data
public class AttrGroupWithAttrsVO {
    /** 分组 ID，指向 {@code pms_attr_group.attr_group_id}。 */
    @TableId
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

    /** 该分组已绑定的属性；没有绑定时为空列表。 */
    private List<AttrEntity> attrs;
}
