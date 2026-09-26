package coupon.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.io.Serial;
import java.io.Serializable;
import lombok.Data;

/**
 * 秒杀活动商品关联，对应 {@code sms_seckill_sku_relation} 表：指定某个场次下参与秒杀的 SKU、秒杀价、放量与本场限购数。
 *
 * <p>活动与场次分别由 {@link SeckillPromotionEntity} 和 {@link SeckillSessionEntity} 描述。
 */
@Data
@TableName("sms_seckill_sku_relation")
public class SeckillSkuRelationEntity implements Serializable {
	@Serial private static final long serialVersionUID = 1L;

	/**
	 * 主键。
	 */
	@TableId
	private Long id;
	/**
	 * 所属秒杀活动 ID。
	 */
	private Long promotionId;
	/**
	 * 所属秒杀场次 ID。
	 */
	private Long promotionSessionId;
	/**
	 * 参与秒杀的商品 SKU ID。
	 */
	private Long skuId;
	/**
	 * 秒杀价。
	 */
	private BigDecimal seckillPrice;
	/**
	 * 本场秒杀总量。
	 */
	private BigDecimal seckillCount;
	/**
	 * 每个会员在本场的限购数量。
	 */
	private BigDecimal seckillLimit;
	/**
	 * 同一场次内的排序值。
	 */
	private Integer seckillSort;

}
