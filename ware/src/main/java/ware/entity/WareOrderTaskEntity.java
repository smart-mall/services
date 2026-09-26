package ware.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 库存工作单，对应 {@code wms_ware_order_task} 表，一次订单的库存锁定任务对应一张单，
 * 明细挂在它下面。
 *
 * <p>解锁时按订单号反查工作单，再逐条处理其明细。
 */
@Data
@TableName("wms_ware_order_task")
public class WareOrderTaskEntity implements Serializable {
	@Serial private static final long serialVersionUID = 1L;

	/** 主键 ID。 */
	@TableId
	private Long id;
	/** 订单 ID；建单时不写，关联以 {@code orderSn} 为准。 */
	private Long orderId;
	/** 订单号，解锁时按它反查工作单。 */
	private String orderSn;
	/** 收货人姓名。 */
	private String consignee;
	/** 收货人电话。 */
	private String consigneeTel;
	/** 收货地址。 */
	private String deliveryAddress;
	/** 订单备注。 */
	private String orderComment;
	/** 付款方式：1 在线付款，2 货到付款。 */
	private Integer paymentWay;
	/** 工作单状态；建单时不写，服务内部也没有读取。 */
	private Integer taskStatus;
	/** 订单描述。 */
	private String orderBody;
	/** 物流单号。 */
	private String trackingNo;
	/** 工作单创建时间。 */
	private Date createTime;
	/** 仓库 ID。 */
	private Long wareId;
	/** 工作单备注。 */
	private String taskComment;

}
