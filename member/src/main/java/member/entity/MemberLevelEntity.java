package member.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.io.Serial;import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 会员等级，对应 {@code ums_member_level} 表。
 *
 * <p>定义各等级的成长值门槛、免运费标准与特权开关，会员归属哪个等级由 {@code ums_member.level_id} 记录。
 */
@Data
@TableName("ums_member_level")
public class MemberLevelEntity implements Serializable {
	@Serial private static final long serialVersionUID = 1L;

	/** 主键。 */
	@TableId
	private Long id;
	/** 等级名称，如普通会员、金牌会员。 */
	private String name;
	/** 达到该等级所需的成长值门槛。 */
	private Integer growthPoint;
	/** 是否为默认等级 [0-不是，1-是]；注册时新会员取该等级。 */
	private Integer defaultStatus;
	/** 免运费门槛金额。 */
	private BigDecimal freeFreightPoint;
	/** 每次评价可获得的成长值。 */
	private Integer commentGrowthPoint;
	/** 是否享有免邮特权 [0-否，1-是]。 */
	private Integer priviledgeFreeFreight;
	/** 是否享有会员价特权 [0-否，1-是]。 */
	private Integer priviledgeMemberPrice;
	/** 是否享有生日特权 [0-否，1-是]。 */
	private Integer priviledgeBirthday;
	/** 备注。 */
	private String note;

}
