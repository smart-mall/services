package auth.vo;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.hibernate.validator.constraints.Length;


/**
 * 注册入参。前端以 JSON 提交（见 {@code SmsAuthController#register}）。
 */
@Data
public class UserRegisterVo {

    @NotEmpty(message = "用户名不能为空")
    @Length(min = 6, max = 19, message="用户名长度在6-19字符")
    private String userName;

    @NotEmpty(message = "密码必须填写")
    @Length(min = 6,max = 18,message = "密码必须是6—18位字符")
    private String password;

    @NotEmpty(message = "密码必须填写")
    @Length(min = 6,max = 18,message = "密码必须是6—18位字符")
    private String password2;

    @NotEmpty(message = "手机号不能为空")
    @Pattern(regexp = "^[1]([3-9])[0-9]{9}$", message = "手机号格式不正确")
    private String phone;

    @NotEmpty(message = "验证码不能为空")
    private String code;

    /**
     * 是否同意协议。
     *
     * <p>原来是 {@code String} + {@code @Pattern(regexp = "^on$")}，那是跟着 HTML 表单来的：
     * 表单里的 checkbox 提交上来是字符串 "on"。换成 JSON 之后前端传的是布尔 true，
     * 拿 "on" 去匹配永远失败，注册会一直卡在"必须同意协议"。</p>
     *
     * <p>{@code @NotNull} 不能省：按 Bean Validation 规范 {@code @AssertTrue} 对 null
     * <b>视为通过</b>，只写它的话，前端漏传 agreement 就能绕过"同意协议"。</p>
     */
    @NotNull(message = "必须同意协议")
    @AssertTrue(message = "必须同意协议")
    private Boolean agreement;

}
