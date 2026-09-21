package auth.vo;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;


/**
 * 邮箱验证码登录入参：邮箱 + 验证码，不需要密码。
 *
 * <p>和账号密码登录（{@link UserLoginVo}）是两条不同的链路，所以单独一个 VO。</p>
 */
@Data
public class UserEmailLoginVo {

    @NotEmpty(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    @NotEmpty(message = "验证码不能为空")
    private String code;
}
