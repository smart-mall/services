package member.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 积分变动流水，对应 {@code ums_integration_change_history} 表。
 *
 * <p>会员积分每增减一次落一条记录，{@code changeCount} 的正负即增减方向。
 *
 * <p>{@code orderSn} 与 {@code changeType} 一起构成唯一索引 {@code uk_order_change}：
 * 一张订单的每类变动最多一条，支付回调或关单消息重投时靠它判定这一次变动是否已经落过账。
 */
@Data
@TableName("ums_integration_change_history")
public class IntegrationChangeHistoryEntity implements Serializable {
	@Serial private static final long serialVersionUID = 1L;

	/** 主键。 */
	@TableId
	private Long id;
	/** 所属会员 ID。 */
	private Long memberId;
	/** 流水生成时间。 */
	private Date createTime;
	/** 变动值，正数表示增加、负数表示扣减。 */
	private Integer changeCount;
	/**
	 * 关联订单号。
	 *
	 * <p>下单抵扣、关单退回、支付赠送三类变动由订单驱动，必定带值；管理员手工调整这类
	 * 没有订单来源的变动为 {@code null}。
	 */
	private String orderSn;
	/**
	 * 变动类型，取值见 {@link member.enums.IntegrationChangeTypeEnum}。
	 *
	 * <p>非订单变动为 {@code null} —— 与 {@code orderSn} 同时为 {@code null} 时不参与唯一索引约束。
	 */
	private Integer changeType;
	/** 备注。 */
	private String note;
	/** 积分来源 [0-购物，1-管理员修改，2-活动]。 */
	private Integer sourceType;

}
