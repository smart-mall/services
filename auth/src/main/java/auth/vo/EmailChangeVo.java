package auth.vo;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

/**
 * 换绑邮箱的入参，与 {@link MobileChangeVo} 对称：没有 memberId，改的是哪个账号由 token 决定。
 */
@Data
public class EmailChangeVo {

    /** 新邮箱，换绑成功后作为登录凭据 */
    @NotEmpty(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    /** 发送到新邮箱的验证码 */
    @NotEmpty(message = "验证码不能为空")
    private String code;
}
