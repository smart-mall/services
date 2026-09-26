package member.vo;

import lombok.Data;


/**
 * 账号密码注册入参。
 *
 * <p>只有账号和密码：手机号、邮箱由各自的验证码链路负责，这里不碰。</p>
 *
 * <p>auth 的 {@code UserAccountVo} 会原样作为请求体发过来，两边字段名必须一致，
 * 否则 Jackson 按名字匹配不上，传过来就是 null。</p>
 */
@Data
public class MemberUserRegisterVo {

    /** 注册账号，对应 {@code ums_member.username}。 */
    private String username;

    /** 注册密码。 */
    private String password;

}
