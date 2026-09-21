package auth.vo;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.hibernate.validator.constraints.Length;


/**
 * 手机验证码链路的入参，登录即注册，所以只有这一个 VO。
 *
 * <p>和 {@link UserEmailVo} 完全对称：{@code username} 只在新建账号时用到，
 * 老用户按手机号认人。</p>
 *
 * <p>手机号格式在这里校验有一个额外好处：<b>落库前一定校验过了</b>。
 * {@code sendCode} 那个接口的参数校验只是为了少发几条垃圾短信，
 * 真正保证 {@code ums_member.mobile} 干净的是这里。</p>
 */
@Data
public class UserMobileVo {

    @NotEmpty(message = "账号不能为空")
    @Length(min = 6, max = 19, message = "账号长度在6-19字符")
    private String username;

    @NotEmpty(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String mobile;

    @NotEmpty(message = "验证码不能为空")
    private String code;

}
