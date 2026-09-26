package ware.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * SKU 库存，对应 {@code wms_ware_sku} 表，一行是某个 SKU 在某个仓库的库存。
 *
 * <p>可售数量 = {@code stock - stockLocked}，锁定与解锁都靠 SQL 在数据库侧做增量。
 */
@Data
@TableName("wms_ware_sku")
public class WareSkuEntity implements Serializable {
	@Serial private static final long serialVersionUID = 1L;

	/** 主键 ID。 */
	@TableId
	private Long id;
	/** SKU 标识。 */
	private Long skuId;
	/** 库存所在仓库 ID。 */
	private Long wareId;
	/** 库存总数，即这个仓库实际有多少件；可售数量 = stock - stockLocked。 */
	private Integer stock;
	/** SKU 名称，建库存行时从商品服务取，取不到时为 {@code null}。 */
	private String skuName;
	/** 已被订单锁定、还没发货的数量；这部分仍在 stock 里，但不能被别的订单再锁。 */
	private Integer stockLocked;

	/** 仓库名，非数据库字段，列表查询时按 wareId 补齐。 */
	@TableField(exist=false)
	private String wareName;

}
