package member.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serial;import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 会员登录记录，对应 {@code ums_member_login_log} 表。
 *
 * <p>每次登录落一条，记录来源 IP、城市与登录端类型。
 */
@Data
@TableName("ums_member_login_log")
public class MemberLoginLogEntity implements Serializable {
	@Serial private static final long serialVersionUID = 1L;

	/** 主键。 */
	@TableId
	private Long id;
	/** 登录会员的 ID。 */
	private Long memberId;
	/** 登录时间。 */
	private Date createTime;
	/** 登录来源 IP。 */
	private String ip;
	/** 登录来源城市。 */
	private String city;
	/** 登录端类型 [1-Web 端，2-App 端]。 */
	private Integer loginType;

}
