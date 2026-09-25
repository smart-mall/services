package auth.vo;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 换绑手机号的入参。
 *
 * <p>只有新号码和验证码，没有 memberId —— 改的是哪个账号由 token 决定。</p>
 *
 * <p>验证码和登录用的是同一个 key（{@code sms:code:<新手机号>}），所以格式校验必须和
 * {@link UserMobileVo#mobile} 一致，否则同一个号在发码时通过、在换绑时被拒。</p>
 */
@Data
public class MobileChangeVo {

    @NotEmpty(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String mobile;

    @NotEmpty(message = "验证码不能为空")
    private String code;
}
