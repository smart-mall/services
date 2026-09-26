package product.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * SPU 规格参数值，对应 {@code pms_product_attr_value} 表：保存每个 SPU 在基本属性上的实际取值。
 *
 * <p>属性名在写入时从 {@link AttrEntity} 复制一份，商品详情与检索索引直接读本表，不再回查属性表。
 */
@Data
@TableName("pms_product_attr_value")
public class ProductAttrValueEntity implements Serializable {
	@Serial private static final long serialVersionUID = 1L;

	/**
	 * 主键。
	 */
	@TableId
	private Long id;
	/**
	 * 所属 SPU ID，指向 {@code pms_spu_info.id}。
	 */
	private Long spuId;
	/**
	 * 属性 ID，指向 {@code pms_attr.attr_id}。
	 */
	private Long attrId;
	/**
	 * 属性名快照。
	 */
	private String attrName;
	/**
	 * 属性值。
	 */
	private String attrValue;
	/**
	 * 同一 SPU 内的展示顺序。
	 */
	private Integer attrSort;
	/**
	 * 快速展示[0-否，1-是]：是否展示在商品介绍上。
	 */
	private Integer quickShow;

}
