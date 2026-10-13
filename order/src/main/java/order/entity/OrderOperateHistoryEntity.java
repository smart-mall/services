package order.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 订单操作历史表 {@code oms_order_operate_history} 的记录，逐条留痕订单的状态变更、操作人与备注。
 *
 * <p>记录由订单流程在变更生效时写入；{@code statusText} 不落库，由查询侧组装。
 */
@Data
@TableName("oms_order_operate_history")
public class OrderOperateHistoryEntity implements Serializable {
	@Serial private static final long serialVersionUID = 1L;

	/** 主键。 */
	@TableId
	private Long id;
	/** 所属订单 ID。 */
	private Long orderId;
	/** 操作人[用户；系统；后台管理员]。 */
	private String operateMan;
	/** 操作时间。 */
	private Date createTime;
	/** 订单状态，取值以 {@link order.enums.OrderStatusEnum} 为准。 */
	private Integer orderStatus;
	/** 操作备注。 */
	private String note;

	/**
	 * 状态文案，给前端直接显示。
	 *
	 * <p>不落库（{@code exist = false}）：它就是 {@link order.enums.OrderStatusEnum} 的措辞，
	 * 在数据库里再存一份等于多一个会和枚举漂移的副本。</p>
	 */
	@TableField(exist = false)
	private String statusText;

}
