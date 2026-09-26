package coupon.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 秒杀活动，对应 {@code sms_seckill_promotion} 表：定义一次秒杀活动的标题与起止日期。
 *
 * <p>活动的场次与商品分别由 {@link SeckillSessionEntity} 和 {@link SeckillSkuRelationEntity} 描述。
 */
@Data
@TableName("sms_seckill_promotion")
public class SeckillPromotionEntity implements Serializable {
	@Serial private static final long serialVersionUID = 1L;

	/** 主键。 */
	@TableId
	private Long id;
	/** 活动标题。 */
	private String title;
	/** 活动开始日期。 */
	private Date startTime;
	/** 活动结束日期。 */
	private Date endTime;
	/** 上下线状态。 */
	private Integer status;
	/** 活动创建时间。 */
	private Date createTime;
	/** 创建人用户 ID。 */
	private Long userId;

	/** 创建人名称，非数据库字段，用于列表回显。 */
	@TableField(exist = false)
	private String userName;

}
