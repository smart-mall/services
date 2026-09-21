package auth.vo;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import org.hibernate.validator.constraints.Length;


/**
 * 邮箱验证码链路的入参，登录即注册，所以只有这一个 VO。
 *
 * <p>{@code username} 只在「这个邮箱还没注册过、需要新建账号」时才会被用到；
 * 老用户走这条链路时是按邮箱认人的，填错账号也不影响登录。</p>
 */
@Data
public class UserEmailVo {

    @NotEmpty(message = "账号不能为空")
    @Length(min = 6, max = 19, message = "账号长度在6-19字符")
    private String username;

    @NotEmpty(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    @NotEmpty(message = "验证码不能为空")
    private String code;

}
