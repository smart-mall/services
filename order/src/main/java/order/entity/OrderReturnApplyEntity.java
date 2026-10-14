package order.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 退货申请表 {@code oms_order_return_apply} 的记录，一笔退货申请对应一行。
 */
@Data
@TableName("oms_order_return_apply")
public class OrderReturnApplyEntity implements Serializable {
	@Serial private static final long serialVersionUID = 1L;

	/** 主键。 */
	@TableId
	private Long id;
	/** 所属订单 ID。 */
	private Long orderId;
	/** 退货商品的 SKU ID。 */
	private Long skuId;
	/** 订单号。 */
	private String orderSn;
	/**
	 * 售后类型[0->仅退款；1->退货退款]，取值见 {@link order.enums.AfterSaleTypeEnum}。
	 *
	 * <p>两条链共用本表：仅退款是整单退钱、不涉及具体商品，所以 {@code skuId}、退货数量、
	 * 退货人与商家收货信息这些列都留 {@code null}（不适用）；退货退款按订单项发起，这些列才有值。
	 * 两个业务共用一张申请表、靠这一列分开，而不是靠"某个字段是不是 NULL"去猜。
	 */
	private Integer type;
	/** 申请时间。 */
	private Date createTime;
	/** 会员用户名。 */
	private String memberUsername;
	/** 退款金额。 */
	private BigDecimal returnAmount;
	/** 退货人姓名。 */
	private String returnName;
	/** 退货人电话。 */
	private String returnPhone;
	/**
	 * 申请状态[0->待处理；1->退货中；2->已完成；3->已拒绝]，取值见
	 * {@link order.enums.AfterSaleStatusEnum}。
	 *
	 * <p>仅退款的单不经过 1：待处理(0) 直接到已完成(2) 或已拒绝(3)。退货的单必经 1（退货中），
	 * 因为"审核通过"与"商家收到货"是两件事。
	 */
	private Integer status;
	/** 处理时间。 */
	private Date handleTime;
	/** 商品图片。 */
	private String skuImg;
	/** 商品名称。 */
	private String skuName;
	/** 商品品牌。 */
	private String skuBrand;
	/** 商品销售属性(JSON)。 */
	private String skuAttrsVals;
	/** 退货数量。 */
	private Integer skuCount;
	/** 商品单价。 */
	private BigDecimal skuPrice;
	/** 商品实际支付单价。 */
	private BigDecimal skuRealPrice;
	/** 退货原因。 */
	private String reason;
	/** 退货问题的补充描述。 */
	private String description;
	/** 凭证图片，以逗号隔开。 */
	private String descPics;
	/** 处理备注。 */
	private String handleNote;
	/** 处理人员。 */
	private String handleMan;
	/** 收货人。 */
	private String receiveMan;
	/** 收货时间。 */
	private Date receiveTime;
	/** 收货备注。 */
	private String receiveNote;
	/** 收货电话。 */
	private String receivePhone;
	/** 公司收货地址。 */
	private String companyAddress;

	/** 售后类型文案，查询时填充，不落库。 */
	@TableField(exist = false)
	private String typeText;

	/** 申请状态文案，查询时填充，不落库。 */
	@TableField(exist = false)
	private String statusText;

}
