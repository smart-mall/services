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
 *
 * <p>收货人、配送地址、订单备注与付款方式在 lockStock 时从 order 侧传来的订单快照写入，
 * 工作单因此是自包含的，发货流程不必回查订单。
 */
@Data
@TableName("wms_ware_order_task")
public class WareOrderTaskEntity implements Serializable {
	@Serial private static final long serialVersionUID = 1L;

	/** 主键 ID。 */
	@TableId
	private Long id;
	/** 订单 ID，建单时由 order 侧带过来。 */
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
	/**
	 * 仓库 ID；建单时不写。
	 *
	 * <p>一张工作单会跨仓库 —— 每个 SKU 各自挑一个有货的仓锁定 —— 工作单级放不下这个事实，
	 * 所以仓库记在明细上。
	 */
	private Long wareId;
	/** 工作单备注。 */
	private String taskComment;

}
