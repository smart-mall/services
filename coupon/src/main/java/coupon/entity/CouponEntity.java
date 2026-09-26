package coupon.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 优惠券模板，对应 {@code sms_coupon} 表：定义券的类型、面额、发放总量，以及可领取与可使用的时间区间。
 *
 * <p>会员领取后在使用侧生成 {@link CouponHistoryEntity} 记录，本表只保存模板与各项统计计数。
 */
@Data
@TableName("sms_coupon")
public class CouponEntity implements Serializable {
	@Serial private static final long serialVersionUID = 1L;

	/**
	 * 主键。
	 */
	@TableId
	private Long id;
	/**
	 * 优惠券类型[0->全场赠券；1->会员赠券；2->购物赠券；3->注册赠券]
	 */
	private Integer couponType;
	/**
	 * 优惠券图片的地址。
	 */
	private String couponImg;
	/**
	 * 优惠券名称，展示给会员。
	 */
	private String couponName;
	/**
	 * 优惠券数量。
	 */
	private Integer num;
	/**
	 * 券面金额，核销时按此金额抵扣。
	 */
	private BigDecimal amount;
	/**
	 * 每个会员最多可领取的张数。
	 */
	private Integer perLimit;
	/**
	 * 使用门槛金额，订单金额达到该值才可使用。
	 */
	private BigDecimal minPoint;
	/**
	 * 优惠券可使用的开始时间。
	 */
	private Date startTime;
	/**
	 * 优惠券可使用的结束时间。
	 */
	private Date endTime;
	/**
	 * 适用范围类型[0->全场通用；1->指定分类；2->指定商品]
	 */
	private Integer useType;
	/**
	 * 备注。
	 */
	private String note;
	/**
	 * 已发行数量。
	 */
	private Integer publishCount;
	/**
	 * 已核销数量。
	 */
	private Integer useCount;
	/**
	 * 已领取数量。
	 */
	private Integer receiveCount;
	/**
	 * 可领取的开始时间。
	 */
	private Date enableStartTime;
	/**
	 * 可领取的结束时间。
	 */
	private Date enableEndTime;
	/**
	 * 优惠码。
	 */
	private String code;
	/**
	 * 可领取的会员等级[0->不限等级，其他-对应等级]
	 */
	private Integer memberLevel;
	/**
	 * 发布状态[0-未发布，1-已发布]
	 */
	private Integer publish;

}
