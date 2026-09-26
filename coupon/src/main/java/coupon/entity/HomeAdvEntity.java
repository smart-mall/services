package coupon.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 首页轮播广告，对应 {@code sms_home_adv} 表：一条记录是轮播位上的一个广告，含图片与跳转地址。
 *
 * <p>投放窗口由开始、结束时间界定，展示顺序由排序值决定。
 */
@Data
@TableName("sms_home_adv")
public class HomeAdvEntity implements Serializable {
	@Serial private static final long serialVersionUID = 1L;

	/** 主键。 */
	@TableId
	private Long id;
	/** 广告名称。 */
	private String name;
	/** 广告图片地址。 */
	private String pic;
	/** 投放开始时间。 */
	private Date startTime;
	/** 投放结束时间。 */
	private Date endTime;
	/** 广告启用状态。 */
	private Integer status;
	/** 广告累计点击次数。 */
	private Integer clickCount;
	/** 广告详情页跳转地址。 */
	private String url;
	/** 备注。 */
	private String note;
	/** 排序值，控制展示顺序。 */
	private Integer sort;
	/** 发布者用户 ID。 */
	private Long publisherId;
	/** 审核者用户 ID。 */
	private Long authId;

}
