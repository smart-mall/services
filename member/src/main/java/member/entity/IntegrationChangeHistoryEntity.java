package member.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 积分变动流水，对应 {@code ums_integration_change_history} 表。
 *
 * <p>会员积分每增减一次落一条记录，{@code changeCount} 的正负即增减方向。
 */
@Data
@TableName("ums_integration_change_history")
public class IntegrationChangeHistoryEntity implements Serializable {
	@Serial private static final long serialVersionUID = 1L;

	/** 主键。 */
	@TableId
	private Long id;
	/** 所属会员 ID。 */
	private Long memberId;
	/** 流水生成时间。 */
	private Date createTime;
	/** 变动值，正数表示增加、负数表示扣减。 */
	private Integer changeCount;
	/** 备注。 */
	private String note;
	/** 积分来源 [0-购物，1-管理员修改，2-活动]。 */
	private Integer sourceTyoe;

}
