package coupon.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 秒杀商品通知订阅，对应 {@code sms_seckill_sku_notice} 表：会员预约某个场次的商品后，开抢前按约定方式提醒。
 *
 * <p>订阅对象由场次 ID 与商品 SKU ID 共同确定。
 */
@Data
@TableName("sms_seckill_sku_notice")
public class SeckillSkuNoticeEntity implements Serializable {
	@Serial private static final long serialVersionUID = 1L;

	/** 主键。 */
	@TableId
	private Long id;
	/** 订阅该提醒的会员 ID。 */
	private Long memberId;
	/** 被订阅的秒杀商品 SKU ID。 */
	private Long skuId;
	/** 商品所属的秒杀场次 ID。 */
	private Long sessionId;
	/** 订阅时间。 */
	private Date subscribeTime;
	/** 提醒实际发送时间，为空表示尚未发送。 */
	private Date sendTime;
	/** 通知方式[0-短信，1-邮件]。 */
	private Integer noticeType;

}
