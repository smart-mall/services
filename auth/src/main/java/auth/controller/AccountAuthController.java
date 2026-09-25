package auth.controller;

import auth.feign.MemberFeignService;
import auth.service.LoginLogService;
import auth.vo.UserAccountVo;
import com.alibaba.fastjson.TypeReference;
import common.exception.BaseException;
import common.utils.JwtUtils;
import common.utils.R;
import common.vo.MemberResponseVo;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 账号密码这条链路：注册 + 登录。和邮箱/手机验证码两条链路各自独立，互不授予登录能力。 */
@Slf4j
@RestController
@RequestMapping("auth/front/account")
public class AccountAuthController extends AbstractLoginController {

    private final MemberFeignService memberFeignService;

    public AccountAuthController(MemberFeignService memberFeignService, JwtUtils jwtUtils,
                                 LoginLogService loginLogService) {
        super(jwtUtils, loginLogService);
        this.memberFeignService = memberFeignService;
    }

    /**
     * 注册。参数校验交给 {@code @Valid}，失败时由 GlobalExceptionHandler 统一转成
     * {@code code:10001 + errors{字段:消息}}。
     */
    @PostMapping("/register")
    public R register(@RequestBody @Valid UserAccountVo vo) {
        // 不打明文密码，只记账号
        log.info("账号注册: username={}", vo.getUsername());

        R register = memberFeignService.accountRegister(vo);
        if (register.getCode() != 0) {
            // 15001 账号已被占用，msg 已经是给人看的中文，直接透传
            log.warn("账号注册失败: code={}, msg={}", register.getCode(), register.getMsg());
            return R.error(register.getCode(), register.getMsg());
        }

        return R.ok();
    }

    /**
     * 登录，成功后签发 JWT。
     *
     * <p>只按账号查人 —— 以前是 {@code username = ? OR mobile = ?}，
     * 也就是手机号可以当账号用；现在手机号只能走短信链路。</p>
     */
    @PostMapping("/login")
    public R login(@RequestBody @Valid UserAccountVo vo, HttpServletRequest request) {
        // 不打明文密码，只记账号
        log.info("账号登录: username={}", vo.getUsername());

        R login = memberFeignService.accountLogin(vo);
        if (login.getCode() != 0) {
            // 15003 账号或密码错误
            log.warn("账号登录失败: username={}, code={}", vo.getUsername(), login.getCode());
            return R.error(login.getCode(), login.getMsg());
        }

        MemberResponseVo user = login.getData("data", new TypeReference<MemberResponseVo>() {});
        if (user == null || user.getId() == null) {
            log.error("member 返回登录成功但用户信息不完整: {}", login);
            throw new BaseException("登录失败，用户信息异常");
        }

        return issueToken(user, request);
    }
}
