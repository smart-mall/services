package order.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serial;import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 订单操作历史表 {@code oms_order_operate_history} 的记录，逐条留痕订单的状态变更、操作人与备注。
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
	/** 订单状态，取值以 {@link order.enume.OrderStatusEnum} 为准。 */
	private Integer orderStatus;
	/** 操作备注。 */
	private String note;

}
