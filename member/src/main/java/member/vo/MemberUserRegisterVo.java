package member.vo;

import lombok.Data;


/**
 * 账号密码注册入参。
 *
 * <p>只有账号和密码两个字段：手机号、邮箱都不在这里 ——
 * 短信/邮箱两条验证码链路各自负责自己的联系方式，账号密码这条链路不碰它们。</p>
 *
 * <p>字段名是 {@code username}（和 {@code ums_member.username} 对齐）。
 * auth 的 {@code UserAccountVo} 会原样作为请求体发过来，两边字段名必须一致，
 * 否则 Jackson 按名字匹配不上、传过来就是 null。</p>
 */
@Data
public class MemberUserRegisterVo {

    private String username;

    private String password;

}
