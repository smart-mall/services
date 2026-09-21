package auth.controller;

import auth.feign.MemberFeignService;
import auth.feign.ThirdPartFeignService;
import auth.utils.VerifyCodeUtils;
import auth.vo.UserEmailLoginVo;
import auth.vo.UserEmailRegisterVo;
import com.alibaba.fastjson.TypeReference;
import common.constant.AuthServerConstant;
import common.exception.BaseCodeEnum;
import common.exception.BaseException;
import common.utils.JwtUtils;
import common.utils.R;
import common.vo.MemberResponseVo;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 邮箱这条链路：发码 + 注册 + 验证码登录。
 *
 * <p>和手机号那条（{@link SmsAuthController}）是刻意的对称设计，差别在于两点：</p>
 * <ol>
 *   <li>验证码不是只服务注册 —— 邮箱登录本身也是「邮箱 + 验证码」（不需要密码），
 *       所以 {@link #login} 也要校验验证码；而账号密码登录在 {@link AccountAuthController}。</li>
 *   <li>发信走 Resend，实现在 third-party 服务的 {@code EmailSendController}，
 *       auth 只负责生成验证码、存 Redis、防刷，然后通过 Feign 让 third-party 去发。</li>
 * </ol>
 *
 * <p>Redis 里的 key 是 {@code email:code:<邮箱>}，value 是 {@code 验证码_写入时间戳}，
 * 和短信共用同一套「60 秒防刷 + 五分钟有效 + 用过即废」的机制 ——
 * 这套机制整个收在 {@link VerifyCodeUtils} 里，本类只负责挑选 key 前缀和决定何时消耗验证码。</p>
 *
 * <p>路径前缀 {@code auth/email} 配合网关的 {@code Path=/api/auth/**} 路由，
 * 所以前端写 {@code /api/auth/email/sendCode}，到这里就是 {@code /auth/email/sendCode}。</p>
 */
@Slf4j
@RestController
@RequestMapping("auth/email")
public class EmailAuthController {

    private final ThirdPartFeignService thirdPartFeignService;
    private final MemberFeignService memberFeignService;
    private final StringRedisTemplate stringRedisTemplate;
    private final JwtUtils jwtUtils;

    public EmailAuthController(ThirdPartFeignService thirdPartFeignService,
                               MemberFeignService memberFeignService,
                               StringRedisTemplate stringRedisTemplate,
                               JwtUtils jwtUtils) {
        this.thirdPartFeignService = thirdPartFeignService;
        this.memberFeignService = memberFeignService;
        this.stringRedisTemplate = stringRedisTemplate;
        this.jwtUtils = jwtUtils;
    }

    /**
     * 发送邮箱验证码。
     *
     * <p>和短信那个接口的防刷逻辑完全一致（都在 {@link VerifyCodeUtils} 里），只是 key 前缀换成
     * {@code email:code:}。
     * 注意不能拿 {@code @Email} 之类的注解来校验这个参数（那是给请求体用的），
     * 真发不出去的时候 Resend 会返回 422，这里会把原因带回去。</p>
     */
    @GetMapping("/sendCode")
    public R sendCode(@RequestParam("email") String email) {
        log.info("发送邮箱验证码: {}", email);
        int time = 5;

        //1、接口防刷：同一个邮箱 60 秒内只能发一次
        if (VerifyCodeUtils.remainingSeconds(stringRedisTemplate, AuthServerConstant.EMAIL_CODE_CACHE_PREFIX, email) > 0) {
            return R.error(BaseCodeEnum.SMS_CODE_EXCEPTION.getCode(), BaseCodeEnum.SMS_CODE_EXCEPTION.getMsg());
        }

        //2、生成验证码、存 Redis（time 分钟），再交给 third-party 用 Resend 把邮件发出去
        VerifyCodeUtils.send(stringRedisTemplate, AuthServerConstant.EMAIL_CODE_CACHE_PREFIX, email, time, codeNum -> {
            R r = thirdPartFeignService.emailSendCode(email, codeNum);
            if (r.getCode() != 0) {
                // 这里必须抛出来：不然验证码已经写进 Redis、邮件却没发出去，前端还以为发出去了
                throw new BaseException("邮件发送失败: " + r.getMsg());
            }
        });

        return R.ok();
    }

    /**
     * 邮箱注册。
     *
     * <p>校验分两层，返回结构和手机号注册完全一致，前端只要认 {@code errors} 一个地方：
     * 字段本身不合法（用户名长度、邮箱格式、没勾协议……）由 {@code @Valid} +
     * GlobalExceptionHandler 统一转成 {@code code:10001 + errors{字段:消息}}；
     * 跨字段的规则（两次密码不一致、验证码不对）在这里手工返回同样的结构。</p>
     */
    @PostMapping("/register")
    public R register(@RequestBody @Valid UserEmailRegisterVo vo) {
        // 不打明文密码
        log.info("邮箱注册: userName={}, email={}", vo.getUserName(), vo.getEmail());

        if (!Objects.equals(vo.getPassword(), vo.getPassword2())) {
            return fieldError("password2", "两次输入的密码不一致");
        }

        //1、校验验证码（只读不写，等业务成功之后才决定删不删）
        if (!VerifyCodeUtils.verify(stringRedisTemplate, AuthServerConstant.EMAIL_CODE_CACHE_PREFIX,
                vo.getEmail(), vo.getCode())) {
            return fieldError("code", "验证码错误");
        }

        //2、真正注册，调 member 服务
        R register = memberFeignService.emailRegister(vo);
        if (register.getCode() != 0) {
            // 15001 用户名重复 / 15006 邮箱重复，msg 已经是给人看的中文，直接透传。
            // 和手机号注册一样，这里故意不删验证码，业务失败时用户能用同一个码重试
            log.warn("邮箱注册失败: code={}, msg={}", register.getCode(), register.getMsg());
            return R.error(register.getCode(), register.getMsg());
        }

        //3、注册成功才删掉验证码（一次性）
        VerifyCodeUtils.consume(stringRedisTemplate, AuthServerConstant.EMAIL_CODE_CACHE_PREFIX, vo.getEmail());

        return R.ok();
    }

    /**
     * 邮箱 + 验证码登录，成功后签发 JWT。
     *
     * <p>返回结构和账号密码登录一致：{@code {code:0, msg:"success", data:{token, expiresIn, user}}}，
     * 前端照样是 {@code setToken(res.data.data.token)}。</p>
     *
     * <p>这个邮箱还没注册过时返回 15007（"该邮箱尚未注册，请先注册"），
     * 并且<b>不</b>消耗验证码 —— 用户接下来去注册页还能用同一个码。</p>
     */
    @PostMapping("/login")
    public R login(@RequestBody @Valid UserEmailLoginVo vo) {
        log.info("邮箱验证码登录: email={}", vo.getEmail());

        //1、校验验证码（只读不写，等登录成功之后才决定删不删）
        if (!VerifyCodeUtils.verify(stringRedisTemplate, AuthServerConstant.EMAIL_CODE_CACHE_PREFIX,
                vo.getEmail(), vo.getCode())) {
            return fieldError("code", "验证码错误");
        }

        //2、按邮箱取会员（验证码已经在这边校验过了，member 只负责取人）
        R memberR = memberFeignService.emailLogin(vo.getEmail());
        if (memberR.getCode() != 0) {
            log.warn("邮箱登录失败: code={}, msg={}", memberR.getCode(), memberR.getMsg());
            return R.error(memberR.getCode(), memberR.getMsg());
        }

        MemberResponseVo user = memberR.getData("data", new TypeReference<MemberResponseVo>() {});
        if (user == null || user.getId() == null) {
            log.error("member 返回成功但用户信息不完整: {}", memberR);
            throw new BaseException("登录失败，用户信息异常");
        }

        // 和 AccountAuthController#login 同理：这两个字段不能出这个类
        user.setPassword(null);
        user.setAccessToken(null);

        //3、登录成功才删验证码
        VerifyCodeUtils.consume(stringRedisTemplate, AuthServerConstant.EMAIL_CODE_CACHE_PREFIX, vo.getEmail());

        String token = jwtUtils.create(user);

        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("expiresIn", jwtUtils.getExpireSeconds());
        data.put("user", user);

        log.info("邮箱登录成功: memberId={}, 有效期={}秒", user.getId(), jwtUtils.getExpireSeconds());
        return R.ok().setData(data);
    }

    /** 拼一个和 GlobalExceptionHandler 一模一样的校验错误结构，免得前端要认两种格式 */
    private R fieldError(String field, String message) {
        Map<String, String> errors = new HashMap<>();
        errors.put(field, message);
        return R.error(BaseCodeEnum.VALID_EXCEPTION.getCode(), BaseCodeEnum.VALID_EXCEPTION.getMsg())
                .put("errors", errors);
    }
}
