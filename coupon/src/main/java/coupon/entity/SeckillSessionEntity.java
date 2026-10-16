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
 * 秒杀场次，对应 {@code sms_seckill_session} 表：一场就是一段具体的起止时刻，起点到终点之间可抢购。
 *
 * <p>它是秒杀唯一的归属层：场次下的商品列表由查询时装配到 {@code relationSkus}，不落库。
 */
@Data
@TableName("sms_seckill_session")
public class SeckillSessionEntity implements Serializable {
	@Serial private static final long serialVersionUID = 1L;

	/** 主键。 */
	@TableId
	private Long id;
	/** 场次名称。 */
	private String name;
	/** 开始时间。 */
	private Date startTime;
	/** 结束时间。 */
	private Date endTime;
	/** 启用状态。 */
	private Integer status;
	/** 场次创建时间。 */
	private Date createTime;

	/** 该场次下的秒杀商品列表，非数据库字段，查询时装配。 */
	@TableField(exist = false)
	private List<SeckillSkuRelationEntity> relationSkus;

}
