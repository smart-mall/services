package member.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serial;import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 会员账号，对应 {@code ums_member} 表。
 *
 * <p>保存登录凭证与个人资料，并记录等级、积分、成长值等会员权益状态，是注册登录链路与下单身份的主体。
 */
@Data
@TableName("ums_member")
public class MemberEntity implements Serializable {
	@Serial private static final long serialVersionUID = 1L;

	/** 主键。 */
	@TableId
	private Long id;
	/** 所属会员等级 ID，指向 {@code ums_member_level}；注册时取默认等级，取不到则为 {@code null}。 */
	private Long levelId;
	/** 登录账号，注册时校验全局唯一。 */
	private String username;
	/** 登录密码，存 BCrypt 哈希；验证码链路建的账号没有密码，为 {@code null}。 */
	private String password;
	/** 昵称，注册时默认与账号同名。 */
	private String nickname;
	/** 手机号码，手机号验证码登录时按它认人。 */
	private String mobile;
	/** 邮箱，邮箱验证码登录时按它认人。 */
	private String email;
	/** 头像地址。 */
	private String header;
	/** 性别 [0-未知，1-男，2-女]。 */
	private Integer gender;
	/** 生日。 */
	private Date birth;
	/** 所在城市。 */
	private String city;
	/** 职业。 */
	private String job;
	/** 个性签名。 */
	private String sign;
	/** 会员来源，区分账号由哪个渠道产生。 */
	private Integer sourceType;
	/** 会员积分余额，下单时可抵扣；允许为 {@code null}，消费方需按 0 处理。 */
	private Integer integration;
	/** 成长值累计值，等级门槛见 {@code ums_member_level.growth_point}。 */
	private Integer growth;
	/** 账号启用状态，取值由后台约定；注册链路不写入该字段，新建账号为 {@code null}。 */
	private Integer status;
	/** 注册时间。 */
	private Date createTime;
	/** 社交账号 ID，社交登录时按它匹配已有会员。 */
	private String socialUid;
	/** 社交账号访问令牌，调用社交平台接口时使用。 */
	private String accessToken;
	/** 社交账号令牌有效期，单位秒，由社交平台返回。 */
	private String expiresIn;

}
