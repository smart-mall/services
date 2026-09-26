package order.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serial;import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * Seata 分支事务回滚日志表 {@code undo_log} 的记录，用于分布式事务回滚。
 *
 * <p>本模块只提供通用增删改查与后台分页查询，不参与回滚逻辑。
 */
@Data
@TableName("undo_log")
public class UndoLogEntity implements Serializable {
	@Serial private static final long serialVersionUID = 1L;

	/** 主键。 */
	@TableId
	private Long id;
	/** 分支事务 ID。 */
	private Long branchId;
	/** 全局事务 ID。 */
	private String xid;
	/** 回滚数据的序列化上下文。 */
	private String context;
	/** 回滚所需的原始数据，序列化后的二进制内容。 */
	private byte[] rollbackInfo;
	/** 回滚日志状态。 */
	private Integer logStatus;
	/** 回滚日志创建时间。 */
	private Date logCreated;
	/** 回滚日志修改时间。 */
	private Date logModified;
	/** 预留的扩展字段。 */
	private String ext;

}
