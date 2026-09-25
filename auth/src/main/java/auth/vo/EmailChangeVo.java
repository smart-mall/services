package auth.vo;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

/**
 * 换绑邮箱的入参，与 {@link MobileChangeVo} 对称。
 */
@Data
public class EmailChangeVo {

    @NotEmpty(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    @NotEmpty(message = "验证码不能为空")
    private String code;
}
