package common.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.ToString;

import java.io.Serial;import java.io.Serializable;
import java.util.Date;


@ToString
@Data
public class MemberResponseVo implements Serializable {

    private static final long serialVersionUID = 5573669251256409786L;

    private Long id;
    /**
     * 会员等级id
     */
    private Long levelId;
    /**
     * 用户名
     */
    private String username;
    /**
     * 密码
     *
     * <p>必须忽略 JSON 序列化：从 member 服务登录返回的是完整 MemberEntity，
     * 这里拿到的是 BCrypt 哈希，而登录接口要把它作为 data 返回给前端。</p>
     *
     * <p>只挡得住 Jackson（Spring MVC 出参用它）。auth 用 fastjson 的
     * {@code R.getData(...)} 把它从 member 的响应里反序列化出来时，
     * fastjson 不认这个注解，password 照样会有值 —— 所以别指望它来防内部泄露，
     * 往 token 和请求头里塞字段时一律手写白名单（见 JwtUtils.create / LoginUserUtils.encode）。</p>
     */
    @JsonIgnore
    private String password;
    /**
     * 昵称
     */
    private String nickname;
    /**
     * 手机号码
     */
    private String mobile;
    /**
     * 邮箱
     */
    private String email;
    /**
     * 头像
     */
    private String header;
    /**
     * 性别
     */
    private Integer gender;
    /**
     * 生日
     */
    private Date birth;
    /**
     * 所在城市
     */
    private String city;
    /**
     * 职业
     */
    private String job;
    /**
     * 个性签名
     */
    private String sign;
    /**
     * 用户来源
     */
    private Integer sourceType;
    /**
     * 积分
     */
    private Integer integration;
    /**
     * 成长值
     */
    private Integer growth;
    /**
     * 启用状态
     */
    private Integer status;
    /**
     * 注册时间
     */
    private Date createTime;

    /**
     * 社交登录UID
     */
    private String socialUid;

    /**
     * 社交登录TOKEN
     *
     * <p>同上：拿到它就能冒充用户去调微博的接口，不能出现在响应体里。</p>
     */
    @JsonIgnore
    private String accessToken;

    /**
     * 社交登录过期时间
     *
     * <p>也是微博那套的字段，SPA 用不到。更要紧的是它是基本类型 long，
     * 序列化出来永远是 {@code "expiresIn":0}，而登录响应里 data.expiresIn 是 JWT 的有效期（604800），
     * 两个同名字段在同一个响应里、值却一个是 0 一个是 604800，前端极容易取错，索性直接不输出。</p>
     */
    @JsonIgnore
    private long expiresIn;

}
