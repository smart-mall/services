package coupon.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 秒杀活动场次，对应 {@code sms_seckill_session} 表：把一天切成若干可抢购的时间段，供秒杀商品挂载。
 *
 * <p>场次下的商品列表由查询时装配到 {@code relationSkus}，不落库。
 */
@Data
@TableName("sms_seckill_session")
public class SeckillSessionEntity implements Serializable {
	@Serial private static final long serialVersionUID = 1L;

	/**
	 * 主键。
	 */
	@TableId
	private Long id;
	/**
	 * 场次名称。
	 */
	private String name;
	/**
	 * 每日开始时间。
	 */
	private Date startTime;
	/**
	 * 每日结束时间。
	 */
	private Date endTime;
	/**
	 * 启用状态。
	 */
	private Integer status;
	/**
	 * 场次创建时间。
	 */
	private Date createTime;

	/**
	 * 该场次下的秒杀商品列表，非数据库字段，查询时装配。
	 */
	@TableField(exist = false)
	private List<SeckillSkuRelationEntity> relationSkus;

}
