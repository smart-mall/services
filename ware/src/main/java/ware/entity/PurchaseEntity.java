package ware.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * 采购单：把同一仓库的采购需求合并成一张单，派给采购员后由其领取并提交采购结果。
 *
 * <p>对应 {@code wms_purchase} 表；总金额与仓库由明细重算，不接收前端传值。
 */
@Data
@TableName("wms_purchase")
public class PurchaseEntity implements Serializable {
	@Serial private static final long serialVersionUID = 1L;

	/** 主键 ID。 */
	@TableId
	private Long id;
	/** 采购员（后台管理员）ID；为空表示还没分配采购员。 */
	private Long assigneeId;
	/** 采购员姓名，列表查询按它做关键字匹配。 */
	private String assigneeName;
	/** 采购员联系电话。 */
	private String phone;
	/** 优先级，必须大于 0；合并需求单时未指定则新建的采购单取 1。 */
	private Integer priority;
	/** 采购单状态：0 新建，1 已分配，2 已领取，3 已完成，4 有异常。 */
	private Integer status;
	/** 仓库 ID；一张采购单只对应一个仓库，由明细的仓库重算。 */
	private Long wareId;
	/** 采购单总金额，按明细的采购金额逐条累加。 */
	private BigDecimal amount;
	/** 创建时间。 */
	private Date createTime;
	/** 最后更新时间。 */
	private Date updateTime;

	/** 仓库名，非数据库字段，列表查询时按 wareId 补齐。 */
	@TableField (exist = false)
	private String wareName;

	/** 当前状态下允许的操作（assign/receive/done/delete），由列表接口填充，前端按钮据此显示。 */
	@TableField(exist = false)
	private List<String> allowedActions;
}
