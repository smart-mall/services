package product.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 商品三级分类
 * 
 * @author ??
 * @email sunlightcs@gmail.com
 * @date 2025-09-15 09:58:33
 */
@Data
@TableName("pms_category")
public class CategoryEntity implements Serializable {
	@Serial
	private static final long serialVersionUID = 1L;

	/**
	 * 分类id
	 */
	@TableId
	private Long catId;
	/**
	 * 分类名称
	 */
	private String name;
	/**
	 * 父分类id
	 */
	private Long parentCid;
	/**
	 * 层级
	 */
	private Integer catLevel;
	/**
	 * 是否显示[0-不显示，1显示]。不是删除标记，删除是物理删，所以这里没有 @TableLogic。
	 * 需要按它过滤的只有前台导航，见 CategoryServiceImpl#getCatalogTree。
	 */
	private Integer showStatus;
	/**
	 * 排序
	 */
	private Integer sort;
	/**
	 * Element UI 图标类名，如 el-icon-goods。只用于后台分类树的渲染，商城前台不用
	 */
	private String icon;
	/**
	 * 计量单位
	 */
	private String productUnit;
	/**
	 * 商品数量
	 */
	private Integer productCount;
	/**
	 * 子分类
	 */
	@TableField(exist = false)
	private List<CategoryEntity> children;

}
