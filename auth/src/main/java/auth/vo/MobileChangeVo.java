package auth.vo;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 换绑手机号的入参。
 *
 * <p>没有 memberId，改的是哪个账号由 token 决定；验证码与登录共用
 * {@code sms:code:<新手机号>} 这个 key，格式校验必须与 {@link UserMobileVo#mobile} 一致。
 */
@Data
public class MobileChangeVo {

    /** 新手机号，换绑成功后作为登录凭据。 */
    @NotEmpty(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String mobile;

    /** 发送到新手机号的验证码。 */
    @NotEmpty(message = "验证码不能为空")
    private String code;
}
