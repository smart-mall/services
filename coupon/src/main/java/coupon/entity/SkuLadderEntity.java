package coupon.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 商品阶梯价，对应 {@code sms_sku_ladder} 表：按购买件数分档给出折扣价。
 *
 * <p>能否与其它优惠叠加由 {@code addOther} 决定。
 */
@Data
@TableName("sms_sku_ladder")
public class SkuLadderEntity implements Serializable {
	@Serial private static final long serialVersionUID = 1L;

	/**
	 * 主键。
	 */
	@TableId
	private Long id;
	/**
	 * 适用阶梯价的商品 SKU ID。
	 */
	private Long skuId;
	/**
	 * 达到该购买件数才适用本档折扣。
	 */
	private Integer fullCount;
	/**
	 * 折扣率，如 0.92 表示九二折。
	 */
	private BigDecimal discount;
	/**
	 * 本档的折后单价。
	 */
	private BigDecimal price;
	/**
	 * 能否与其它优惠叠加[0-不可叠加，1-可叠加]
	 */
	private Integer addOther;

}
