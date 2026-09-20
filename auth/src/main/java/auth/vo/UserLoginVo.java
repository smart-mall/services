package auth.vo;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;


@Data
public class UserLoginVo {

    /** 登录账号：member 服务用它去匹配 username 或 mobile */
    @NotEmpty(message = "用户名或手机号不能为空")
    private String loginacct;

    @NotEmpty(message = "密码不能为空")
    private String password;
}
