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
 * 品牌，对应 {@code pms_brand} 表：商品的品牌信息，同时作为前台按品牌筛选的依据。
 *
 * <p>字段上的校验注解按 {@code AddGroup} 与 {@code UpdateGroup} 分组生效，新增与修改走不同的必填规则。
 */
@Data
@TableName("pms_brand")
public class BrandEntity implements Serializable {
	@Serial private static final long serialVersionUID = 1L;

	/**
	 * 主键，由数据库生成，新增时不允许传入。
	 */
	@TableId
	@NotNull(groups = {UpdateGroup.class}, message = "品牌id不能为空")
	@Null(groups = {AddGroup.class}, message = "新增时不能指定品牌id")
	private Long brandId;
	/**
	 * 品牌名。
	 */
	@NotBlank(message = "品牌名不能为空")
	private String name;
	/**
	 * 品牌 logo 地址，必须是合法的 URL。
	 */
	@NotBlank(message = "品牌logo不能为空")
	@URL(message = "品牌logo必须是合法的URL")
	private String logo;
	/**
	 * 品牌介绍。
	 */
	@NotBlank(message = "介绍不能为空")
	private String descript;
	/**
	 * 显示状态[0-不显示，1-显示]。
	 */
	@NotNull(message = "显示状态不能为空")
	@Min(value = 0, message = "显示状态只能是0或1")
	@Max(value = 1, message = "显示状态只能是0或1")
	private Integer showStatus;
	/**
	 * 检索首字母，单个英文字母。
	 */
	@NotBlank(message = "检索首字母不能为空")
	@Pattern(regexp = "^[a-zA-Z]$", message = "检索首字母只能是单个英文字母")
	private String firstLetter;
	/**
	 * 排序值。
	 */
	@NotNull(message = "排序不能为空")
	@Min(value = 0, message = "排序不能小于0")
	private Integer sort;

}
