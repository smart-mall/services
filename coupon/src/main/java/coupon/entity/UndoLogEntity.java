package coupon.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 分布式事务回滚日志，对应 {@code undo_log} 表：记录分支事务的数据修改镜像，供全局事务回滚时还原。
 *
 * <p>全局事务 ID 与分支事务 ID 组成唯一索引，共同定位一条回滚记录。
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
	 * 分支事务 ID，与全局事务 ID 共同定位一条回滚记录。
	 */
	private Long branchId;
	/**
	 * 全局事务 ID。
	 */
	private String xid;
	/**
	 * 回滚上下文，与回滚镜像一并持久化。
	 */
	private String context;
	/**
	 * 回滚镜像的二进制数据，保存数据被修改前后的行值。
	 */
	private byte[] rollbackInfo;
	/**
	 * 回滚日志状态。
	 */
	private Integer logStatus;
	/**
	 * 回滚日志创建时间。
	 */
	private Date logCreated;
	/**
	 * 回滚日志最后修改时间。
	 */
	private Date logModified;
	/**
	 * 扩展字段。
	 */
	private String ext;

}
