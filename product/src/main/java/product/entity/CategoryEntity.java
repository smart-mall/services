package product.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 商品三级分类，对应 {@code pms_category} 表：一级到三级分类的树形结构，商品与属性分组都挂在三级分类上。
 *
 * <p>{@code parentCid} 为 0 表示一级分类，{@code children} 非数据库字段，由服务层组装成树。
 */
@Data
@TableName("pms_category")
public class CategoryEntity implements Serializable {
	@Serial
	private static final long serialVersionUID = 1L;

	/**
	 * 主键。
	 */
	@TableId
	private Long catId;
	/**
	 * 分类名称。
	 */
	private String name;
	/**
	 * 父分类 ID，一级分类为 0。
	 */
	private Long parentCid;
	/**
	 * 分类层级，一级分类为 1，最深到三级。
	 */
	private Integer catLevel;
	/**
	 * 是否显示[0-不显示，1-显示]。本表是物理删除，没有 {@code @TableLogic} 逻辑删标记；
	 * 只有前台导航按它过滤，见 {@code CategoryServiceImpl#getCatalogTree}。
	 */
	private Integer showStatus;
	/**
	 * 同一父分类下的展示顺序。
	 */
	private Integer sort;
	/**
	 * Element UI 图标类名，如 el-icon-goods，只用于后台分类树渲染。
	 */
	private String icon;
	/**
	 * 计量单位。
	 */
	private String productUnit;
	/**
	 * 该分类下的商品数量。
	 */
	private Integer productCount;
	/**
	 * 子分类；非数据库字段，用于组装分类树。
	 */
	@TableField(exist = false)
	private List<CategoryEntity> children;

}
