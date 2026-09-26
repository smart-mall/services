package coupon.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 商品会员价，对应 {@code sms_member_price} 表：为某个 SKU 按会员等级设置专属价格。
 *
 * <p>同一 SKU 可按不同会员等级各配置一行价格。
 */
@Data
@TableName("sms_member_price")
public class MemberPriceEntity implements Serializable {
	@Serial private static final long serialVersionUID = 1L;

	/** 主键。 */
	@TableId
	private Long id;
	/** 商品 SKU ID。 */
	private Long skuId;
	/** 会员等级 ID。 */
	private Long memberLevelId;
	/** 会员等级名称。 */
	private String memberLevelName;
	/** 该等级对应的会员价。 */
	private BigDecimal memberPrice;
	/** 能否与其它优惠叠加[0-不可叠加优惠，1-可叠加]。 */
	private Integer addOther;

	/** SKU 名称，非数据库字段，用于列表回显。 */
	@TableField(exist = false)
	private String skuName;

}
