package coupon.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 优惠券领取记录，对应 {@code sms_coupon_history} 表：会员每领取一张券生成一行，并跟踪该券的使用状态。
 *
 * <p>券的模板信息保存在 {@link CouponEntity}，本表只记录领取与核销结果。一个会员可以有多行指向同一张券
 * （每人限领可能大于 1），所以业务上认的"一张券"是本表的主键，不是 {@code couponId}。
 *
 * <p>{@code couponName} 等带 {@link TableField} 标记的字段不落库，由查询侧按 {@code couponId}
 * 回查模板后组装，只为免掉前端再发一次请求。
 */
@Data
@TableName("sms_coupon_history")
public class CouponHistoryEntity implements Serializable {
	@Serial private static final long serialVersionUID = 1L;

	/** 主键，业务上用它指代"一张具体的券"。 */
	@TableId
	private Long id;
	/**
	 * 所领优惠券的模板 ID，关联 {@code sms_coupon.id}。
	 */
	private Long couponId;
	/** 领取该券的会员 ID。 */
	private Long memberId;
	/** 领取时的会员昵称。 */
	private String memberNickName;
	/** 领取方式[0->后台赠送；1->主动领取]。 */
	private Integer getType;
	/** 领取时间。 */
	private Date createTime;
	/**
	 * 使用状态[0->未使用；1->已使用；2->已过期；3->占用中]，取值见 {@code CouponUseStatusEnum}。
	 *
	 * <p>占用中表示已随某张订单锁定、尚未付款。它与模板上的 {@code useType} 同名不同义：
	 * 后者是"能买什么"的适用范围。
	 */
	private Integer useType;
	/** 核销时间，未核销时为空。 */
	private Date useTime;
	/** 占用或核销这张券的订单 ID，未占用时为空。 */
	private Long orderId;
	/**
	 * 占用或核销这张券的订单号，未占用时为空。
	 *
	 * <p>类型与拥有方一致：{@code oms_order.order_sn} 是字符串，订单号由 {@code IdWorker} 生成，
	 * 虽然当前恰好是纯数字，但按数值存会在换成含字母的号段时直接溢出。
	 */
	private String orderSn;

	/** 券名称，不落库，由查询侧回查模板组装。 */
	@TableField(exist = false)
	private String couponName;

	/** 券面金额，不落库，由查询侧回查模板组装。 */
	@TableField(exist = false)
	private BigDecimal amount;

	/** 使用门槛，不落库，由查询侧回查模板组装。 */
	@TableField(exist = false)
	private BigDecimal minPoint;

	/** 券的可使用开始时间，不落库，由查询侧回查模板组装。 */
	@TableField(exist = false)
	private Date couponStartTime;

	/** 券的可使用结束时间，不落库，由查询侧回查模板组装。 */
	@TableField(exist = false)
	private Date couponEndTime;

	/** 使用状态文案，不落库，由 {@code useType} 现算，免掉前端再维护一份映射。 */
	@TableField(exist = false)
	private String useTypeText;

}
