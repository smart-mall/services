package product.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;import java.io.Serializable;

/**
 * SKU 图片，对应 {@code pms_sku_images} 表：保存每个 SKU 的图集，其中一张标记为默认图。
 */
@Data
@TableName("pms_sku_images")
public class SkuImagesEntity implements Serializable {
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
	 * 图片地址。
	 */
	private String imgUrl;
	/**
	 * 展示顺序。
	 */
	private Integer imgSort;
	/**
	 * 默认图[0-不是默认图，1-是默认图]。
	 */
	private Integer defaultImg;

}
