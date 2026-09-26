package ware.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 采购需求单：一行表示某个 SKU 在某仓库的采购需求，并入采购单后由采购员执行。
 *
 * <p>对应 {@code wms_purchase_detail} 表；{@code purchaseId} 为空表示这条需求还没并入采购单。
 */
@Data
@TableName("wms_purchase_detail")
public class PurchaseDetailEntity implements Serializable {
	@Serial private static final long serialVersionUID = 1L;

	/** 主键 ID。 */
	@TableId
	private Long id;
	/** 所属采购单 ID；为空表示还没并入任何采购单。 */
	private Long purchaseId;
	/** 采购的 SKU 标识。 */
	private Long skuId;
	/** 采购数量。 */
	private Integer skuNum;
	/** 这条需求的采购金额（总额，不是单价），采购单的总金额由它逐条累加。 */
	private BigDecimal skuPrice;
	/** 入库仓库 ID；同一张采购单下的明细必须属于同一个仓库。 */
	private Long wareId;
	/** 需求单状态：0 新建，1 已分配，2 正在采购，3 已完成，4 采购失败。 */
	private Integer status;

	/** SKU 名称，非数据库字段；列表查询时按 skuId 远程取，取不到为 {@code null}。 */
	@TableField(exist = false)
	private String skuName =  "";

	/** 仓库名，非数据库字段；列表查询时按 wareId 补齐，查不到为空串。 */
	@TableField(exist = false)
	private String wareName = "";

	/** 当前状态下允许的操作（edit/delete/merge/unassign），由列表接口填充，前端按钮据此显示。 */
	@TableField(exist = false)
	private List<String> allowedActions;

}
