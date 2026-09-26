package coupon.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 首页专题，对应 {@code sms_home_subject} 表：每个专题是首页上的一个入口，点击后进入专题页展示其下商品。
 *
 * <p>专题包含哪些商品由 {@link HomeSubjectSpuEntity} 描述。
 */
@Data
@TableName("sms_home_subject")
public class HomeSubjectEntity implements Serializable {
	@Serial private static final long serialVersionUID = 1L;

	/**
	 * 主键。
	 */
	@TableId
	private Long id;
	/**
	 * 专题名称。
	 */
	private String name;
	/**
	 * 专题标题。
	 */
	private String title;
	/**
	 * 专题副标题。
	 */
	private String subTitle;
	/**
	 * 显示状态。
	 */
	private Integer status;
	/**
	 * 专题详情页跳转地址。
	 */
	private String url;
	/**
	 * 排序值，控制展示顺序。
	 */
	private Integer sort;
	/**
	 * 专题图片地址。
	 */
	private String img;

}
