package member.vo;

import lombok.Data;


/**
 * 账号密码登录入参。
 *
 * <p>{@code username} 只匹配账号，不会回退到手机号：手机号登录走短信验证码那条链路。</p>
 */
@Data
public class MemberUserLoginVo {

    /** 登录账号。 */
    private String username;

    /** 登录密码。 */
    private String password;

}
