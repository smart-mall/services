package product.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 商品属性，对应 {@code pms_attr} 表：定义某个三级分类下可用于描述商品的属性。
 *
 * <p>{@code attrType} 为 0 的是销售属性，取值按 SKU 记录；为 1 的是基本属性，归入属性分组供详情页展示。
 */
@Data
@TableName("pms_attr")
public class AttrEntity implements Serializable {
	@Serial private static final long serialVersionUID = 1L;

	/**
	 * 主键。
	 */
	@TableId
	private Long attrId;
	/**
	 * 属性名，如颜色、CPU 型号。
	 */
	private String attrName;
	/**
	 * 是否需要检索[0-不需要，1-需要]，为 1 的属性可作为商品列表的筛选条件。
	 */
	private Integer searchType;
	/**
	 * 属性图标地址。
	 */
	private String icon;
	/**
	 * 可选值列表，多个可选值用逗号分隔。
	 */
	private String valueSelect;
	/**
	 * 属性类型[0-销售属性，1-基本属性]。
	 */
	private Integer attrType;
	/**
	 * 属性值类型[0-唯一，1-单选]。
	 */
	private Integer valueType;
	/**
	 * 启用状态[0-禁用，1-启用]。
	 */
	private Long enable;
	/**
	 * 所属分类 ID，指向 {@code pms_category.cat_id}。
	 */
	private Long catalogId;
	/**
	 * 快速展示[0-否，1-是]：是否展示在商品介绍上，在 SKU 中仍可调整。
	 */
	private Integer showDesc;

}
