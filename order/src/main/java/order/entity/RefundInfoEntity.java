package order.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.io.Serial;import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 退款信息表 {@code oms_refund_info} 的记录，保存退货申请对应的退款流水。
 */
@Data
@TableName("oms_refund_info")
public class RefundInfoEntity implements Serializable {
	@Serial private static final long serialVersionUID = 1L;

	/** 主键。 */
	@TableId
	private Long id;
	/** 关联的退货申请 ID，指向 {@code oms_order_return_apply.id}。 */
	private Long orderReturnId;
	/**
	 * 退款金额
	 */
	private BigDecimal refund;
	/**
	 * 退款交易流水号
	 */
	private String refundSn;
	/**
	 * 退款状态
	 */
	private Integer refundStatus;
	/**
	 * 退款渠道[1-支付宝，2-微信，3-银联，4-汇款]
	 */
	private Integer refundChannel;
	/** 退款内容，长文本字段，本模块未解析其结构。 */
	private String refundContent;

}
