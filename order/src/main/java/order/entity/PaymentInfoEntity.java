package order.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 支付信息表 {@code oms_payment_info} 的记录，保存每笔订单的支付流水与回调内容。
 */
@Data
@TableName("oms_payment_info")
public class PaymentInfoEntity implements Serializable {
	@Serial private static final long serialVersionUID = 1L;

	/** 主键。 */
	@TableId
	private Long id;
	/** 订单号（对外业务号）。 */
	private String orderSn;
	/** 关联的订单 ID。 */
	private Long orderId;
	/** 平台交易流水号：支付宝存 {@code trade_no}，微信存 {@code transaction_id}。 */
	private String alipayTradeNo;
	/** 支付金额：支付宝存买家实付 {@code buyer_pay_amount}，微信存订单金额。 */
	private BigDecimal totalAmount;
	/** 交易内容。 */
	private String subject;
	/** 支付状态：支付宝存回调里的 {@code trade_status} 原文，微信与测试支付存渠道侧的约定值。 */
	private String paymentStatus;
	/** 创建时间。 */
	private Date createTime;
	/** 确认时间。 */
	private Date confirmTime;
	/** 回调内容。 */
	private String callbackContent;
	/** 回调时间。 */
	private Date callbackTime;

}
