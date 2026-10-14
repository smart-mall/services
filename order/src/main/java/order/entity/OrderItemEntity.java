package order.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.io.Serial;
import java.io.Serializable;
import lombok.Data;
import common.to.CommentBriefVo;

/**
 * 订单项表 {@code oms_order_item} 的记录，保存订单中每个 SKU 的下单明细。
 *
 * <p>商品名称、图片、价格等字段在下单时写入，是当时的商品快照，不随商品后续修改而变化。
 */
@Data
@TableName("oms_order_item")
public class OrderItemEntity implements Serializable {
	@Serial private static final long serialVersionUID = 1L;

	/** 主键。 */
	@TableId
	private Long id;
	/** 所属订单 ID。 */
	private Long orderId;
	/** 所属订单号。 */
	private String orderSn;
	/** 商品 SPU ID。 */
	private Long spuId;
	/** 商品 SPU 名称。 */
	private String spuName;
	/** 商品 SPU 图片。 */
	private String spuPic;
	/** 商品品牌名称。 */
	private String spuBrand;
	/** 商品分类 ID。 */
	private Long categoryId;
	/** 商品 SKU ID。 */
	private Long skuId;
	/** 商品 SKU 名称。 */
	private String skuName;
	/** 商品 SKU 图片。 */
	private String skuPic;
	/** 商品 SKU 单价。 */
	private BigDecimal skuPrice;
	/** 购买数量。 */
	private Integer skuQuantity;
	/** 商品销售属性组合（JSON）。 */
	private String skuAttrsVals;
	/** 商品促销分解金额。 */
	private BigDecimal promotionAmount;
	/** 优惠券优惠分解金额。 */
	private BigDecimal couponAmount;
	/** 积分优惠分解金额。 */
	private BigDecimal integrationAmount;
	/** 该商品经过优惠后的分解金额。 */
	private BigDecimal realAmount;
	/** 赠送积分。 */
	private Integer giftIntegration;
	/** 赠送成长值。 */
	private Integer giftGrowth;
	/**
	 * 该项的商品评价摘要，供会员端把已评价的入口显示成「查看评价」。
	 *
	 * <p>不落库：订单列表装配时向 product 批量取回，没有评价或取不到时保持 {@code null}。
	 */
	@TableField(exist = false)
	private CommentBriefVo comment;

	/**
	 * 这一项现在能不能发起"退货退款"。
	 *
	 * <p>不落库，由服务端在装配订单时算好：判据是"订单已完成"加"这一项没有正在处理或已完成的退货申请"。
	 * 与 {@link #comment} 同源，都是"入口显不显示由服务端说"，前端不自己拼条件。
	 */
	@TableField(exist = false)
	private Boolean canApplyReturn;

}
