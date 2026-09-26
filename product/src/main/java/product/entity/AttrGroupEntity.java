package product.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 属性分组，对应 {@code pms_attr_group} 表：把同一分类下的基本属性归入若干分组，供商品详情页分组展示规格。
 *
 * <p>分组与属性的关联保存在 {@link AttrAttrgroupRelationEntity}。
 */
@Data
@TableName("pms_attr_group")
public class AttrGroupEntity implements Serializable {
	@Serial private static final long serialVersionUID = 1L;

	/** 主键。 */
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
	/**
	 * 所属分类 ID，指向 {@code pms_category.cat_id}。
	 */
	private Long catalogId;

	/**
	 * 分类完整路径，父分类 ID 依次排列；非数据库字段，由 {@code CategoryService#findcatalogIds} 回填。
	 */
	@TableField(exist = false)
	private List<Long> catalogIds;

}
