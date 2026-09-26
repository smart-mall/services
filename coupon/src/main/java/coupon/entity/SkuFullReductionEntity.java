package coupon.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 商品满减规则，对应 {@code sms_sku_full_reduction} 表：按 SKU 配置满额后减免的促销。
 *
 * <p>能否与其它优惠叠加由 {@code addOther} 决定。
 */
@Data
@TableName("sms_sku_full_reduction")
public class SkuFullReductionEntity implements Serializable {
	@Serial private static final long serialVersionUID = 1L;

	/** 主键。 */
	@TableId
	private Long id;
	/** 参与满减的商品 SKU ID。 */
	private Long skuId;
	/** 满减门槛金额，该 SKU 的金额达到此值才减免。 */
	private BigDecimal fullPrice;
	/** 达到门槛后减免的金额。 */
	private BigDecimal reducePrice;
	/** 能否与其它优惠叠加。 */
	private Integer addOther;

	/** SKU 名称，非数据库字段，用于列表回显。 */
	@TableField(exist = false)
	private String skuName;

}
