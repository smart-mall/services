package auth.vo;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import org.hibernate.validator.constraints.Length;


/**
 * 账号密码链路的入参，<b>注册和登录共用一个</b>。
 *
 * <p>正因为共用，所以没有"确认密码"字段 —— 两次输入是否一致由前端保证，
 * 后端只认最终那一个 password。</p>
 */
@Data
public class UserAccountVo {

    /** 登录账号，注册时同时作为用户名。 */
    @NotEmpty(message = "账号不能为空")
    @Length(min = 6, max = 19, message = "账号长度在6-19字符")
    private String username;

    /** 明文密码，只出现在请求体里，不打进日志。 */
    /** 明文密码，只出现在请求体里，不打进日志。 */
    @NotEmpty(message = "密码必须填写")
    @Length(min = 6, max = 18, message = "密码必须是6—18位字符")
    private String password;

}
