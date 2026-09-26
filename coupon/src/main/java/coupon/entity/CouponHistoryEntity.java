package coupon.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 优惠券领取记录，对应 {@code sms_coupon_history} 表：会员每领取一张券生成一行，并跟踪该券的使用状态。
 *
 * <p>券的模板信息保存在 {@link CouponEntity}，本表只记录领取与核销结果。
 */
@Data
@TableName("sms_coupon_history")
public class CouponHistoryEntity implements Serializable {
	@Serial private static final long serialVersionUID = 1L;

	/**
	 * 主键。
	 */
	@TableId
	private Long id;
	/**
	 * 所领优惠券的模板 ID，关联 {@code sms_coupon.id}。
	 */
	private Long couponId;
	/**
	 * 领取该券的会员 ID。
	 */
	private Long memberId;
	/**
	 * 领取时的会员昵称。
	 */
	private String memberNickName;
	/**
	 * 领取方式[0->后台赠送；1->主动领取]
	 */
	private Integer getType;
	/**
	 * 领取时间。
	 */
	private Date createTime;
	/**
	 * 使用状态[0->未使用；1->已使用；2->已过期]
	 */
	private Integer useType;
	/**
	 * 核销时间，未使用时为空。
	 */
	private Date useTime;
	/**
	 * 核销该券的订单 ID，未使用时为空。
	 */
	private Long orderId;
	/**
	 * 核销该券的订单号，未使用时为空。
	 */
	private Long orderSn;

}
