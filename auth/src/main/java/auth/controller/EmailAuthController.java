package auth.controller;

import auth.feign.MemberFeignService;
import auth.feign.ThirdPartFeignService;
import auth.service.LoginLogService;
import auth.utils.VerifyCodeUtils;
import auth.vo.UserEmailVo;
import common.constant.AuthServerConstant;
import common.exception.BaseCodeEnum;
import common.exception.BaseException;
import common.utils.JwtUtils;
import common.utils.R;
import common.vo.MemberResponseVo;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import common.exception.ValidationException;
/**
 * 邮箱这条链路：发码 + 登录，没有单独的注册接口 —— 验证码校验通过后按邮箱找人，找不到就用
 * 请求里的账号新建一个，所以"登录"本身就兼顾了注册。
 *
 * <p>与账号密码链路独立：这里建出的账号没有密码，只能靠邮箱验证码登录；Redis 里的 key 是
 * {@code email:code:<邮箱>}，value 是 {@code 验证码_写入时间戳}，机制收在 {@link VerifyCodeUtils} 里。
 */
@Slf4j
@RestController
@Validated
@RequestMapping("auth/front/email")
public class EmailAuthController extends AbstractLoginController {

    private final ThirdPartFeignService thirdPartFeignService;
    private final MemberFeignService memberFeignService;
    private final StringRedisTemplate stringRedisTemplate;

    public EmailAuthController(ThirdPartFeignService thirdPartFeignService,
                               MemberFeignService memberFeignService,
                               StringRedisTemplate stringRedisTemplate,
                               JwtUtils jwtUtils,
                               LoginLogService loginLogService) {
        super(jwtUtils, loginLogService);
        this.thirdPartFeignService = thirdPartFeignService;
        this.memberFeignService = memberFeignService;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 发送邮箱验证码。
     *
     * <p>邮箱格式在这里也校验一遍，图的是少发几封垃圾邮件，真正的发信在 third-party（走 Resend）；
     * 给 query 参数加约束需要类上有 {@code @Validated}，失败时抛 {@code ConstraintViolationException}，
     * 由 GlobalExceptionHandler 转成 {@code code:10001 + errors}。</p>
     *
     * @param email 收码邮箱
     * @return 成功返回 {@code code:0}；距上次发码不足 60 秒时返回验证码频率异常
     */
    @GetMapping("/sendCode")
    public R<Void> sendCode(@RequestParam("email")
                      @NotEmpty(message = "邮箱不能为空")
                      @Email(message = "邮箱格式不正确") String email) {
        log.info("发送邮箱验证码: {}", email);
        // 验证码 5 分钟过期，同时也是防刷窗口的上限
        int time = 5;

        // 1. 接口防刷：同一个邮箱 60 秒内只能发一次
        if (VerifyCodeUtils.remainingSeconds(stringRedisTemplate, AuthServerConstant.EMAIL_CODE_CACHE_PREFIX, email) > 0) {
            return R.error(BaseCodeEnum.SMS_CODE_EXCEPTION);
        }

        // 2. 生成验证码存 Redis（time 分钟），再交给 third-party 用 Resend 发信
        VerifyCodeUtils.send(stringRedisTemplate, AuthServerConstant.EMAIL_CODE_CACHE_PREFIX, email, time, codeNum -> {
            R<Void> r = thirdPartFeignService.emailSendCode(email, codeNum);
            if (r.getCode() != 0) {
                // 这里必须抛出来：不然验证码已经写进 Redis、邮件却没发出去，前端还以为发出去了
                throw new BaseException("邮件发送失败: " + r.getMsg());
            }
        });

        return R.ok();
    }

    /**
     * 邮箱验证码登录（兼注册）。
     *
     * <p>失败只有两种：验证码不对（10001 + errors{code}），或需要新建账号时账号已被占用（15001）；
     * 老用户按邮箱认人，请求里的 {@code username} 会被忽略。</p>
     *
     * @param vo      登录入参，含账号、邮箱与验证码
     * @param request 当前请求，用于取客户端 IP
     * @return 登录响应，{@code data} 内含 token、有效期秒数与用户信息
     */
    @PostMapping("/login")
    public R<Map<String, Object>> login(@RequestBody @Valid UserEmailVo vo, HttpServletRequest request) {
        log.info("邮箱验证码登录: email={}", vo.getEmail());

        // 1. 校验验证码：只读不写，登录成功之后才决定删不删
        if (!VerifyCodeUtils.verify(stringRedisTemplate, AuthServerConstant.EMAIL_CODE_CACHE_PREFIX,
                vo.getEmail(), vo.getCode())) {
            throw new ValidationException("code", "验证码错误");
        }

        // 2. 按邮箱取人，取不到就用 username 建一个
        R<MemberResponseVo> memberR = memberFeignService.emailLogin(vo.getUsername(), vo.getEmail());
        if (memberR.getCode() != 0) {
            // 15001 账号已被占用。故意不删验证码：用户换个账号名就能用同一个码重试
            log.warn("邮箱登录失败: code={}, msg={}", memberR.getCode(), memberR.getMsg());
            return R.error(memberR.getCode(), memberR.getMsg());
        }

        MemberResponseVo user = memberR.getData();
        if (user == null || user.getId() == null) {
            log.error("member 返回成功但用户信息不完整: {}", memberR);
            throw new BaseException("登录失败，用户信息异常");
        }

        // 3. 登录成功才删验证码
        VerifyCodeUtils.consume(stringRedisTemplate, AuthServerConstant.EMAIL_CODE_CACHE_PREFIX, vo.getEmail());

        return issueToken(user, request);
    }
}
