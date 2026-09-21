package auth.vo;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.validator.constraints.Length;


/**
 * 邮箱注册入参。前端以 JSON 提交（见 {@code EmailAuthController#register}）。
 *
 * <p>字段和 {@link UserRegisterVo}（手机号注册）一一对应，只是把 {@code phone}
 * 换成了 {@code email} —— 手机号那边校验的是 {@code ^1[3-9]\d{9}$}，这边用 {@link Email}。
 * 这个对象会原样作为 Feign 的请求体发给 member，member 只取 userName / password / email，
 * 多出来的字段会被忽略（Spring Boot 默认关掉了 FAIL_ON_UNKNOWN_PROPERTIES）。</p>
 */
@Data
public class UserEmailRegisterVo {

    @NotEmpty(message = "用户名不能为空")
    @Length(min = 6, max = 19, message="用户名长度在6-19字符")
    private String userName;

    @NotEmpty(message = "密码必须填写")
    @Length(min = 6,max = 18,message = "密码必须是6—18位字符")
    private String password;

    @NotEmpty(message = "密码必须填写")
    @Length(min = 6,max = 18,message = "密码必须是6—18位字符")
    private String password2;

    @NotEmpty(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    @NotEmpty(message = "验证码不能为空")
    private String code;

    /**
     * 是否同意协议。和手机号注册一样，{@code @NotNull} 不能省 ——
     * {@code @AssertTrue} 对 null 视为通过，只写它的话漏传就能绕过。
     */
    @NotNull(message = "必须同意协议")
    @AssertTrue(message = "必须同意协议")
    private Boolean agreement;

}
