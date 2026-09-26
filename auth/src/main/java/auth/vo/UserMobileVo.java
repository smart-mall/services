package auth.vo;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.hibernate.validator.constraints.Length;


/**
 * 手机验证码链路的入参，登录即注册，所以只有这一个 VO。
 *
 * <p>{@code username} 只在新建账号时用到，老用户按手机号认人；手机号格式在这里校验，
 * 落库前一定校验过了，{@code sendCode} 上的校验只为少发几条垃圾短信。
 */
@Data
public class UserMobileVo {

    /** 新建账号时使用的用户名，老用户走本链路时被忽略 */
    @NotEmpty(message = "账号不能为空")
    @Length(min = 6, max = 19, message = "账号长度在6-19字符")
    private String username;

    /** 收码手机号，也是识别账号的依据 */
    @NotEmpty(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String mobile;

    /** 用户提交的短信验证码 */
    @NotEmpty(message = "验证码不能为空")
    private String code;

}
