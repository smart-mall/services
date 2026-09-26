package product.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * SPU 图片，对应 {@code pms_spu_images} 表：保存 SPU 维度的图集，与 SKU 图集分开存放。
 */
@Data
@TableName("pms_spu_images")
public class SpuImagesEntity implements Serializable {
	@Serial private static final long serialVersionUID = 1L;

	/** 主键。 */
	@TableId
	private Long id;
	/**
	 * 所属 SPU ID，指向 {@code pms_spu_info.id}。
	 */
	private Long spuId;
	/** 图片名。 */
	private String imgName;
	/** 图片地址。 */
	private String imgUrl;
	/** 展示顺序。 */
	private Integer imgSort;
	/** 是否为默认图。 */
	private Integer defaultImg;

}
