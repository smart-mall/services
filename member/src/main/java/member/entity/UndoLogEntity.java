package member.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 分布式事务回滚日志，对应 {@code undo_log} 表。
 *
 * <p>Seata 分支事务提交前写入数据镜像，全局事务回滚时据此还原；写入与清理由事务框架完成，业务侧只做查询。
 */
@Data
@TableName("undo_log")
public class UndoLogEntity implements Serializable {
	@Serial private static final long serialVersionUID = 1L;

	/** 主键。 */
	@TableId
	private Long id;
	/** 分支事务 ID，与 {@code xid} 一起唯一标识一条回滚日志。 */
	private Long branchId;
	/** 全局事务 ID。 */
	private String xid;
	/** 回滚上下文。 */
	private String context;
	/** 回滚数据，保存用于还原的序列化内容。 */
	private byte[] rollbackInfo;
	/** 回滚日志状态。 */
	private Integer logStatus;
	/** 日志创建时间。 */
	private Date logCreated;
	/** 日志最后修改时间。 */
	private Date logModified;
	/** 扩展字段。 */
	private String ext;

}
