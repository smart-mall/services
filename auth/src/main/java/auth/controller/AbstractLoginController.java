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
 * 三条登录链路（账号密码、邮箱验证码、手机验证码）共用的收尾动作。
 *
 * <p>只放"登录成功之后"和"跨字段校验失败"这两件事 —— 怎么认证是各 controller 自己的事
 * （密码比对 / 验证码比对），认证通过之后签发 JWT 和拼响应结构完全一样，没必要写三份。</p>
 *
 * <p>路径前缀不在这里定义：三个 controller 各自 {@code @RequestMapping}，
 * 所以它们是三个平级的入口，不是一个继承体系。</p>
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
     * 签发 JWT 并拼出登录响应：{@code {code:0, msg:"success", data:{token, expiresIn, user}}}，
     * 顺带记一条登录记录。
     *
     * <p>为什么 token 放在 data 里再套一层而不是平铺：前端 request.ts 的取值习惯是
     * {@code res.data.data}（对应后端 {@code R.ok(x)}）。</p>
     *
     * <p>入参 {@code user} 会被就地改写（置空两个敏感字段），所以别在调用后再用它。</p>
     */
    protected final R<Map<String, Object>> issueToken(MemberResponseVo user, HttpServletRequest request) {
        // member 返回的是完整 MemberEntity，password 是 BCrypt 哈希，accessToken 是微博令牌。
        // MemberResponseVo 上虽然有 @JsonIgnore，但那只是双保险之一：
        // auth 用 fastjson 的 getData(...) 反序列化时它并不生效，所以这里必须显式置空。
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
