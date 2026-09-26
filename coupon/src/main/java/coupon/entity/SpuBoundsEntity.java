package coupon.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * SPU 积分设置，对应 {@code sms_spu_bounds} 表：规定购买某个 SPU 时赠送的成长积分与购物积分。
 *
 * <p>积分是否赠送由 {@code work} 的四个状态位控制。
 */
@Data
@TableName("sms_spu_bounds")
public class SpuBoundsEntity implements Serializable {
	@Serial private static final long serialVersionUID = 1L;

	/** 主键。 */
	@TableId
	private Long id;
	/** 适用积分规则的 SPU ID。 */
	private Long spuId;
	/** 赠送的成长积分值。 */
	private BigDecimal growBounds;
	/** 赠送的购物积分值。 */
	private BigDecimal buyBounds;
	/**
	 * 优惠生效情况[1111（四个状态位，从右到左）;0 - 无优惠，成长积分是否赠送;1 - 无优惠，购物积分是否赠送;2 - 有优惠，成长积分是否赠送;3 - 有优惠，购物积分是否赠送【状态位0：不赠送，1：赠送】]。
	 */
	private Integer work;

	/** SPU 名称，非数据库字段，用于列表回显。 */
	@TableField(exist = false)
	private String spuName;

}
