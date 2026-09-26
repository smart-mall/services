package product.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;import java.io.Serializable;

/**
 * SKU 销售属性取值，对应 {@code pms_sku_sale_attr_value} 表：记录每个 SKU 在销售属性（如颜色、套餐）上的取值。
 *
 * <p>{@code attrId} 指向 {@code pms_attr} 中 {@code attrType} 为 0 的属性。
 */
@Data
@TableName("pms_sku_sale_attr_value")
public class SkuSaleAttrValueEntity implements Serializable {
	@Serial private static final long serialVersionUID = 1L;

	/**
	 * 主键。
	 */
	@TableId
	private Long id;
	/**
	 * 所属 SKU ID，指向 {@code pms_sku_info.sku_id}。
	 */
	private Long skuId;
	/**
	 * 销售属性 ID，指向 {@code pms_attr.attr_id}。
	 */
	private Long attrId;
	/**
	 * 销售属性名快照。
	 */
	private String attrName;
	/**
	 * 销售属性值。
	 */
	private String attrValue;
	/**
	 * 展示顺序。
	 */
	private Integer attrSort;

}
