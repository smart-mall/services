package order.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.io.Serial;
import java.io.Serializable;
import lombok.Data;

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

}
