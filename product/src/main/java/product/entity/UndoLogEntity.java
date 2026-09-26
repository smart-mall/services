package product.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;import java.io.Serializable;
import java.util.Date;

/**
 * 分布式事务回滚日志，对应 {@code undo_log} 表：Seata AT 模式在分支事务提交前后写入数据快照，回滚时据此还原。
 *
 * <p>本表由 Seata 读写，业务代码不直接操作；{@code xid} 与 {@code branchId} 上有唯一索引。
 */
@Data
@TableName("undo_log")
public class UndoLogEntity implements Serializable {
	@Serial private static final long serialVersionUID = 1L;

	/**
	 * 主键。
	 */
	@TableId
	private Long id;
	/**
	 * 分支事务 ID，Seata 为每个参与全局事务的分支分配。
	 */
	private Long branchId;
	/**
	 * 全局事务 ID，同一次全局事务的各个分支共用。
	 */
	private String xid;
	/**
	 * 回滚日志上下文，记录序列化方式等信息。
	 */
	private String context;
	/**
	 * 回滚数据快照，序列化后的字节数组。
	 */
	private byte[] rollbackInfo;
	/**
	 * 回滚日志状态[0-正常，1-防御状态]。
	 */
	private Integer logStatus;
	/**
	 * 日志创建时间。
	 */
	private Date logCreated;
	/**
	 * 日志最后修改时间。
	 */
	private Date logModified;
	/**
	 * 预留扩展字段。
	 */
	private String ext;

}
