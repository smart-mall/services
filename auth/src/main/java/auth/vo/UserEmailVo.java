package auth.vo;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import org.hibernate.validator.constraints.Length;


/**
 * 邮箱验证码链路的入参，登录即注册，所以只有这一个 VO。
 *
 * <p>{@code username} 只在需要新建账号时用到；老用户按邮箱认人，填错账号也不影响登录。
 */
@Data
public class UserEmailVo {

    /** 新建账号时使用的用户名，老用户走本链路时被忽略。 */
    @NotEmpty(message = "账号不能为空")
    @Length(min = 6, max = 19, message = "账号长度在6-19字符")
    private String username;

    /** 收码邮箱，也是识别账号的依据。 */
    @NotEmpty(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    /** 用户提交的邮箱验证码。 */
    @NotEmpty(message = "验证码不能为空")
    private String code;

}
