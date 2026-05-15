package product.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import common.valid.AddGroup;
import common.valid.UpdateGroup;
import jakarta.validation.constraints.*;
import lombok.Data;
import org.hibernate.validator.constraints.URL;

import java.io.Serial;
import java.io.Serializable;

/**
 * 品牌
 * 
 * @author ??
 * @email sunlightcs@gmail.com
 * @date 2025-09-15 09:58:33
 */
@Data
@TableName("pms_brand")
public class BrandEntity implements Serializable {
	@Serial private static final long serialVersionUID = 1L;

	/**
	 * 品牌id
	 */
	@TableId
	@NotNull(groups = {UpdateGroup.class})
	@Null(groups = {AddGroup.class})
	private Long brandId;
	/**
	 * 品牌名
	 */
	@NotBlank
	private String name;
	/**
	 * 品牌logo地址
	 */
	@NotBlank
	@URL
	private String logo;
	/**
	 * 介绍
	 */
	@NotBlank
	private String descript;
	/**
	 * 显示状态[0-不显示；1-显示]
	 */
	@NotNull
	@Min(value = 0)
	@Max(value = 1)
	private Integer showStatus;
	/**
	 * 检索首字母
	 */
	@NotBlank
	@Pattern(regexp = "^[a-zA-Z]$")
	private String firstLetter;
	/**
	 * 排序
	 */
	@NotNull
	@Min(value = 0)
	private Integer sort;

}
