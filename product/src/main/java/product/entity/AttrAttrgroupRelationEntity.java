package product.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 属性与属性分组的关联，对应 {@code pms_attr_attrgroup_relation} 表：指定基本属性归入哪个属性分组以及组内顺序。
 *
 * <p>只有 {@code attrType} 为 1 的基本属性才会写入本表，销售属性不参与分组。
 */
@Data
@TableName("pms_attr_attrgroup_relation")
public class AttrAttrgroupRelationEntity implements Serializable {
	@Serial private static final long serialVersionUID = 1L;

	/**
	 * 主键。
	 */
	@TableId
	private Long id;
	/**
	 * 属性 ID，指向 {@code pms_attr.attr_id}。
	 */
	private Long attrId;
	/**
	 * 属性分组 ID，指向 {@code pms_attr_group.attr_group_id}。
	 */
	private Long attrGroupId;
	/**
	 * 属性在分组内的展示顺序。
	 */
	private Integer attrSort;

}
