package coupon.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 优惠券与商品 SPU 的关联，对应 {@code sms_coupon_spu_relation} 表：列出限定商品可用的优惠券覆盖了哪些 SPU。
 *
 * <p>仅当优惠券的适用范围为指定商品时本表才有数据。
 */
@Data
@TableName("sms_coupon_spu_relation")
public class CouponSpuRelationEntity implements Serializable {
	@Serial private static final long serialVersionUID = 1L;

	/**
	 * 主键。
	 */
	@TableId
	private Long id;
	/**
	 * 优惠券模板 ID，关联 {@code sms_coupon.id}。
	 */
	private Long couponId;
	/**
	 * 适用商品 SPU ID。
	 */
	private Long spuId;
	/**
	 * SPU 名称，冗余保存便于列表直接展示。
	 */
	private String spuName;

}
