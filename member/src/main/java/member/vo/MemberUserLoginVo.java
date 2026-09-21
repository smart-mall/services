package member.vo;

import lombok.Data;


/**
 * 账号密码登录入参。
 *
 * <p>字段名从 {@code loginacct} 改成了 {@code username}：原来那个名字是 renren 的遗留，
 * 还对应着"能拿手机号当账号登录"的旧行为（{@code username = ? OR mobile = ?}），
 * 现在账号就是账号，和手机号分开了。</p>
 */
@Data
public class MemberUserLoginVo {

    private String username;

    private String password;

}
