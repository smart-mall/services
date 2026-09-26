package common.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;


/**
 * 会员信息：auth 登录成功后作为 data 返回给前端，同时是 JWT claim 与网关注入
 * {@code X-Member-Claims} 头的载荷。
 */
@ToString
@Data
public class MemberResponseVo implements Serializable {

    @Serial private static final long serialVersionUID = 5573669251256409786L;

    /** 会员 ID。 */
    private Long id;
    /** 会员等级 ID。 */
    private Long levelId;
    /** 用户名。 */
    private String username;
    /**
     * 密码，BCrypt 哈希。
     *
     * <p>登录接口要把整个对象作为 data 返回给前端，所以必须忽略 Jackson 序列化。
     * 但 {@code @JsonIgnore} 只挡得住 Jackson：auth 用 fastjson 的 {@code R.getData()}
     * 从 member 的响应里取值时它不生效，因此不能指望它防内部泄露 ——
     * 往 token 与请求头里塞字段一律手写白名单（见 {@code JwtUtils.create}、{@code LoginUserUtils.encode}）。</p>
     */
    @JsonIgnore
    private String password;
    /** 昵称。 */
    private String nickname;
    /** 手机号码。 */
    private String mobile;
    /** 邮箱。 */
    private String email;
    /** 头像地址。 */
    private String header;
    /** 性别。 */
    private Integer gender;
    /** 生日。 */
    private Date birth;
    /** 所在城市。 */
    private String city;
    /** 职业。 */
    private String job;
    /** 个性签名。 */
    private String sign;
    /** 用户来源。 */
    private Integer sourceType;
    /** 积分。 */
    private Integer integration;
    /** 成长值。 */
    private Integer growth;
    /** 启用状态。 */
    private Integer status;
    /** 注册时间。 */
    private Date createTime;

    /** 社交登录 UID。 */
    private String socialUid;

    /**
     * 社交登录 token。
     *
     * <p>拿到它就能冒充用户去调微博的接口，不能出现在响应体里。</p>
     */
    @JsonIgnore
    private String accessToken;

    /**
     * 社交登录过期时间。
     *
     * <p>它是基本类型 long，序列化出来恒为 {@code "expiresIn":0}；而登录响应里
     * {@code data.expiresIn} 是 JWT 的有效期。两个同名字段值不同，前端极易取错，所以直接不输出。</p>
     */
    @JsonIgnore
    private long expiresIn;

}
