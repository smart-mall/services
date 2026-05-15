package product.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;import java.io.Serializable;

/**
 * spu图片
 * 
 * @author ??
 * @email sunlightcs@gmail.com
 * @date 2025-09-15 09:58:33
 */
@Data
@TableName("pms_spu_images")
public class SpuImagesEntity implements Serializable {
	@Serial private static final long serialVersionUID = 1L;

	/**
	 * id
	 */
	@TableId
	private Long id;
	/**
	 * spu_id
	 */
	private Long spuId;
	/**
	 * 图片名
	 */
	private String imgName;
	/**
	 * 图片地址
	 */
	private String imgUrl;
	/**
	 * 顺序
	 */
	private Integer imgSort;
	/**
	 * 是否默认图
	 */
	private Integer defaultImg;

}
