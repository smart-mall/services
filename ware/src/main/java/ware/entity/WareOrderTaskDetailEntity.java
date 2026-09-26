package ware.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;import java.io.Serializable;

/**
 * 库存工作单明细，对应 {@code wms_ware_order_task_detail} 表，一行记录一次成功的库存锁定：
 * 某个 SKU 在某个仓库锁了多少件。
 *
 * <p>解锁按 {@code lockStatus} 判断幂等，只处理仍为已锁定的明细。
 */
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
@TableName("wms_ware_order_task_detail")
public class WareOrderTaskDetailEntity implements Serializable {
	@Serial private static final long serialVersionUID = 1L;

	/** 主键 ID。 */
	@TableId
	private Long id;
	/** 被锁定的 SKU 标识。 */
	private Long skuId;
	/** SKU 名称；建明细时写入空串，真实名称未落库。 */
	private String skuName;
	/** 该明细锁定的数量。 */
	private Integer skuNum;
	/** 所属库存工作单 ID。 */
	private Long taskId;
	/** 锁定库存所在的仓库 ID。 */
	private Long wareId;
	/**
	 * 锁定状态，解锁逻辑的判据：1 已锁定，2 已解锁，3 扣减。
	 *
	 * <p>只有 1 的明细需要解锁，重复消费解锁消息时不会把库存多减一次。
	 */
	private Integer lockStatus;

}
