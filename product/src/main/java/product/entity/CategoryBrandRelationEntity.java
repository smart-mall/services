package product.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serial;import java.io.Serializable;

/**
 * 品牌与分类的关联，对应 {@code pms_category_brand_relation} 表：记录某个分类下有哪些品牌可供选择。
 *
 * <p>品牌名与分类名在此冗余保存，列表查询不必回查 {@link BrandEntity} 与 {@link CategoryEntity}，
 * 改名时由 {@code CategoryBrandRelationService} 同步更新。
 */
@Data
@TableName("pms_category_brand_relation")
public class CategoryBrandRelationEntity implements Serializable {
	@Serial private static final long serialVersionUID = 1L;

	/**
	 * 主键。
	 */
	@TableId
	private Long id;
	/**
	 * 品牌 ID，指向 {@code pms_brand.brand_id}。
	 */
	private Long brandId;
	/**
	 * 分类 ID，指向 {@code pms_category.cat_id}。
	 */
	private Long catalogId;
	/**
	 * 品牌名快照，随品牌重命名同步刷新。
	 */
	private String brandName;
	/**
	 * 分类名快照，随分类重命名同步刷新。
	 */
	private String catalogName;

}
