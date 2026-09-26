package auth.controller;

import auth.feign.MemberFeignService;
import auth.feign.ThirdPartFeignService;
import auth.service.LoginLogService;
import auth.utils.VerifyCodeUtils;
import auth.vo.UserMobileVo;
import common.constant.AuthServerConstant;
import common.exception.BaseCodeEnum;
import common.exception.BaseException;
import common.utils.JwtUtils;
import common.utils.R;
import common.vo.MemberResponseVo;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
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
 * 手机验证码这条链路：发码 + 登录，没有单独的注册接口 —— 按手机号找不到人就新建账号。
 *
 * <p>third-party 里也有 {@code /thirdParty/sms/sendCode}，那个只负责把短信真发出去，别调错。
 */
@Slf4j
@RestController
@Validated
@RequestMapping("auth/front/sms")
public class SmsAuthController extends AbstractLoginController {

    private final ThirdPartFeignService thirdPartFeignService;
    private final MemberFeignService memberFeignService;
    private final StringRedisTemplate stringRedisTemplate;

    public SmsAuthController(ThirdPartFeignService thirdPartFeignService,
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
     * 发送短信验证码。
     *
     * <p>手机号格式在这里也校验一遍，图的是少发几条垃圾短信；真正保证 {@code ums_member.mobile}
     * 干净的是登录接口上的 {@code @Pattern}（见 {@link UserMobileVo}），因为落库发生在那里。</p>
     *
     * @param mobile 收码手机号
     * @return 成功返回 {@code code:0}；距上次发码不足 60 秒时返回验证码频率异常
     */
    @GetMapping("/sendCode")
    public R<Void> sendCode(@RequestParam("mobile")
                      @NotEmpty(message = "手机号不能为空")
                      @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确") String mobile) {
        log.info("发送验证码: {}", mobile);
        // 验证码 5 分钟过期，同时也是防刷窗口的上限
        int time = 5;

        // 1. 接口防刷：同一个手机号 60 秒内只能发一次（防刷窗口和验证码共用 sms:code:<mobile> 这个 key）
        if (VerifyCodeUtils.remainingSeconds(stringRedisTemplate, AuthServerConstant.SMS_CODE_CACHE_PREFIX, mobile) > 0) {
            return R.error(BaseCodeEnum.SMS_CODE_EXCEPTION);
        }

        // 2. 生成验证码存 Redis（time 分钟），再交给 third-party 真正把短信发出去
        VerifyCodeUtils.send(stringRedisTemplate, AuthServerConstant.SMS_CODE_CACHE_PREFIX, mobile, time, codeNum -> {
            R<Void> r = thirdPartFeignService.sendCode(mobile, codeNum, time);
            if (r.getCode() != 0) {
                throw new BaseException("远程服务调用失败" + r.getMsg());
            }
        });

        return R.ok();
    }

    /**
     * 手机验证码登录（兼注册）。
     *
     * <p>失败只有两种：验证码不对（10001 + errors{code}），或需要新建账号时账号已被占用（15001）；
     * 老用户按手机号认人，请求里的 {@code username} 会被忽略。</p>
     *
     * @param vo      登录入参，含账号、手机号与验证码
     * @param request 当前请求，用于取客户端 IP
     * @return 登录响应，{@code data} 内含 token、有效期秒数与用户信息
     */
    @PostMapping("/login")
    public R<Map<String, Object>> login(@RequestBody @Valid UserMobileVo vo, HttpServletRequest request) {
        log.info("手机验证码登录: mobile={}", vo.getMobile());

        // 1. 校验验证码：只读不写，登录成功之后才决定删不删
        if (!VerifyCodeUtils.verify(stringRedisTemplate, AuthServerConstant.SMS_CODE_CACHE_PREFIX,
                vo.getMobile(), vo.getCode())) {
            throw new ValidationException("code", "验证码错误");
        }

        // 2. 按手机号取人，取不到就用 username 建一个
        R<MemberResponseVo> memberR = memberFeignService.mobileLogin(vo.getUsername(), vo.getMobile());
        if (memberR.getCode() != 0) {
            // 15001 账号已被占用。故意不删验证码：用户换个账号名就能用同一个码重试
            log.warn("手机验证码登录失败: code={}, msg={}", memberR.getCode(), memberR.getMsg());
            return R.error(memberR.getCode(), memberR.getMsg());
        }

        MemberResponseVo user = memberR.getData();
        if (user == null || user.getId() == null) {
            log.error("member 返回成功但用户信息不完整: {}", memberR);
            throw new BaseException("登录失败，用户信息异常");
        }

        // 3. 登录成功才删验证码
        VerifyCodeUtils.consume(stringRedisTemplate, AuthServerConstant.SMS_CODE_CACHE_PREFIX, vo.getMobile());

        return issueToken(user, request);
    }
}
