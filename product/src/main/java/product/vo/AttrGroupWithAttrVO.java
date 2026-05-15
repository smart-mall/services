package product.vo;

import lombok.Data;
import product.entity.AttrEntity;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * @author wangwei
 * 2020/10/18 22:19
 */

@Data
public class AttrGroupWithAttrVO implements Serializable {

    @Serial private static final long serialVersionUID = 1L;

    /**
     * 分组id
     */
    private Long attrGroupId;
    /**
     * 组名
     */
    private String attrGroupName;
    /**
     * 排序
     */
    private Integer sort;
    /**
     * 描述
     */
    private String descript;
    /**
     * 组图标
     */
    private String icon;
    /**
     * 所属分类id
     */
    private Long catalogId;

    /**
     * 关联的所有属性
     */
    private List<AttrEntity> attrs;
}
