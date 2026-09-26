package order.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serial;import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 退货原因字典表 {@code oms_order_return_reason} 的记录，保存可选的退货原因及其启用状态。
 */
@Data
@TableName("oms_order_return_reason")
public class OrderReturnReasonEntity implements Serializable {
	@Serial private static final long serialVersionUID = 1L;

	/** 主键。 */
	@TableId
	private Long id;
	/** 退货原因名称。 */
	private String name;
	/** 列表展示顺序。 */
	private Integer sort;
	/** 启用状态。 */
	private Integer status;
	/** 创建时间。 */
	private Date createTime;

}
