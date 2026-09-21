package auth.controller;

import auth.feign.MemberFeignService;
import auth.vo.UserLoginVo;
import com.alibaba.fastjson.TypeReference;
import common.constant.AuthServerConstant;
import common.exception.BaseCodeEnum;
import common.exception.BaseException;
import common.utils.JwtUtils;
import common.utils.LoginUserUtils;
import common.utils.R;
import common.vo.MemberResponseVo;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 账号这条链路：账号密码登录 + 登录态读取（userinfo / logout）。
 *
 * <p>密码登录不属于任何"第三方登录方式"，所以单独放一个 controller；
 * userinfo 和 logout 也放这里 —— 它们跟具体用哪种方式登录无关。</p>
 *
 * <p>路径前缀 {@code auth} 配合网关的 {@code Path=/api/auth/**} 路由
 * （重写规则会把 {@code /api} 去掉，所以前端写 {@code /api/auth/login}，到这里就是 {@code /auth/login}）。</p>
 *
 * <p>错误约定：参数校验失败 → HTTP 200 + {@code code:10001} + {@code errors{字段:消息}}；
 * 业务失败 → HTTP 200 + member 服务给的 code（15003 账号或密码错误）；
 * 未登录 → <b>HTTP 401</b>（前端 request.ts 是看状态码 401 去清 localStorage 里的 token 的，
 * 所以必须是真 401，不能塞在 code 里）。</p>
 */
@Slf4j
@RestController
@RequestMapping("auth")
public class AccountAuthController {

    private final MemberFeignService memberFeignService;
    private final JwtUtils jwtUtils;

    public AccountAuthController(MemberFeignService memberFeignService, JwtUtils jwtUtils) {
        this.memberFeignService = memberFeignService;
        this.jwtUtils = jwtUtils;
    }

    /**
     * 账号密码登录，成功后签发 JWT。
     *
     * <p>返回结构：{@code {code:0, msg:"success", data:{token, expiresIn, user}}}。</p>
     *
     * <p>为什么是 data 里再套一层而不是平铺：前端 request.ts 里写的取值习惯是
     * {@code res.data.data}（对应后端 {@code R.ok().setData(x)}），所以 token 放 data 里最省事。</p>
     */
    @PostMapping("/login")
    public R login(@RequestBody @Valid UserLoginVo vo) {
        // 不打明文密码，只记账号
        log.info("用户登录: loginacct={}", vo.getLoginacct());

        R login = memberFeignService.login(vo);
        if (login.getCode() != 0) {
            // 15003 账号或密码错误
            return R.error(login.getCode(), login.getMsg());
        }

        MemberResponseVo user = login.getData("data", new TypeReference<MemberResponseVo>() {});
        if (user == null || user.getId() == null) {
            log.error("member 返回登录成功但用户信息不完整: {}", login);
            throw new BaseException("登录失败，用户信息异常");
        }

        // member 返回的是完整 MemberEntity，password 是 BCrypt 哈希，accessToken 是微博令牌。
        // MemberResponseVo 上已经加了 @JsonIgnore，这里再显式置空一次：
        // 一是双保险（万一以后有人把出参的序列化换成 fastjson，@JsonIgnore 就不生效了），
        // 二是保证这两个值不会经由 JwtUtils/login 的返回值泄出去。
        user.setPassword(null);
        user.setAccessToken(null);

        String token = jwtUtils.create(user);

        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("expiresIn", jwtUtils.getExpireSeconds());
        data.put("user", user);

        log.info("登录成功: memberId={}, 有效期={}秒", user.getId(), jwtUtils.getExpireSeconds());
        return R.ok().setData(data);
    }

    /**
     * 当前登录用户。
     *
     * <p>SPA 刷新页面之后靠这个接口恢复登录态：有 token 就调它拿用户信息，
     * 拿到 401 就把本地 token 清掉并跳登录页。</p>
     *
     * <p>这里不解析 token —— 网关已经验过签了，用户信息在
     * {@link AuthServerConstant#MEMBER_CLAIMS_HEADER} 请求头里，直接取即可。
     * 也<b>不</b>回查数据库：token 里带了 id/username/nickname/header/integration，
     * 前端要显示的头像和昵称都在里面了。</p>
     */
    @GetMapping("/userinfo")
    public ResponseEntity<R> userinfo(HttpServletRequest request) {
        MemberResponseVo user = LoginUserUtils.currentUser(request);
        if (user == null) {
            return unauthorized(BaseCodeEnum.NOT_LOGIN_EXCEPTION);
        }
        return ResponseEntity.ok(R.ok().setData(user));
    }

    /**
     * 退出登录。
     *
     * <p>JWT 是无状态的，服务端没有会话可清 —— 真正的"退出"是前端把 localStorage 里的 token 丢掉。
     * 留这个接口是为了给前端一个明确的调用点，顺便以后要加黑名单/审计时有地方挂。</p>
     */
    @PostMapping("/logout")
    public R logout() {
        log.info("用户退出登录");
        return R.ok();
    }

    private ResponseEntity<R> unauthorized(BaseCodeEnum codeEnum) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(R.error(codeEnum.getCode(), codeEnum.getMsg()));
    }
}
