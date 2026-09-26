package auth.controller;

import auth.service.LoginLogService;
import common.utils.ClientIpUtils;
import common.utils.JwtUtils;
import common.utils.R;
import common.vo.MemberResponseVo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * 三条登录链路（账号密码、邮箱验证码、手机验证码）共用的收尾动作：签发 JWT、拼登录响应、记登录记录。
 *
 * <p>怎么认证是各 controller 自己的事（密码比对 / 验证码比对）；路径前缀也各自
 * {@code @RequestMapping}，三个 controller 是平级入口，不是一个继承体系。
 */
@Slf4j
public abstract class AbstractLoginController {

    private final JwtUtils jwtUtils;

    private final LoginLogService loginLogService;

    protected AbstractLoginController(JwtUtils jwtUtils, LoginLogService loginLogService) {
        this.jwtUtils = jwtUtils;
        this.loginLogService = loginLogService;
    }

    /**
     * 签发 JWT 并拼出登录响应，顺带记一条登录记录。
     *
     * <p>响应结构为 {@code {code:0, msg:"success", data:{token, expiresIn, user}}}，token 套在
     * data 里是因为前端按 {@code res.data.data} 取值；入参 {@code user} 会被就地改写
     * （置空两个敏感字段），调用之后不要再使用它。</p>
     *
     * @param user    认证通过的会员信息，{@code id} 不能为 {@code null}
     * @param request 当前请求，用于取客户端 IP
     * @return 统一响应结构，{@code data} 内含 token、有效期秒数与用户信息
     */
    protected final R<Map<String, Object>> issueToken(MemberResponseVo user, HttpServletRequest request) {
        // member 返回的是完整 MemberEntity：password 是 BCrypt 哈希，accessToken 是微博令牌。
        // MemberResponseVo 上的 @JsonIgnore 对 fastjson 反序列化不生效，必须在这里显式置空
        user.setPassword(null);
        user.setAccessToken(null);

        String token = jwtUtils.create(user);

        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("expiresIn", jwtUtils.getExpireSeconds());
        data.put("user", user);

        log.info("登录成功: memberId={}, 有效期={}秒", user.getId(), jwtUtils.getExpireSeconds());
        loginLogService.recordWebLogin(user.getId(), ClientIpUtils.currentIp(request));
        return R.ok(data);
    }

}
