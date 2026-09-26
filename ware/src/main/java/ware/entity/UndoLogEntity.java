package ware.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 分布式事务回滚日志，对应 {@code undo_log} 表，一行存放一次分支事务回滚所需的数据。
 *
 * <p>业务代码不写这张表，本模块只提供查询入口。
 */
@Data
@TableName("undo_log")
public class UndoLogEntity implements Serializable {
	@Serial private static final long serialVersionUID = 1L;

	/** 主键 ID。 */
	@TableId
	private Long id;
	/** 分支事务标识，与全局事务标识一起唯一确定一条回滚日志。 */
	private Long branchId;
	/** 全局事务标识。 */
	private String xid;
	/** 事务上下文信息。 */
	private String context;
	/** 回滚所需的原始数据，二进制存放。 */
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
