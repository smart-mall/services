package auth.controller;

import auth.feign.MemberFeignService;
import auth.feign.ThirdPartFeignService;
import auth.utils.VerifyCodeUtils;
import auth.vo.UserRegisterVo;
import common.constant.AuthServerConstant;
import common.exception.BaseCodeEnum;
import common.exception.BaseException;
import common.utils.R;
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
 * 手机验证码这条链路：发码 + 注册。
 *
 * <p>注册之所以跟发码放在一起，是因为它本来就依赖短信验证码（{@code UserRegisterVo}
 * 里有 code 字段，校验不过直接返回"验证码错误"）。所以注册从原来的
 * {@code /api/auth/register} 挪到了 {@code /api/auth/sms/register} 下面。</p>
 *
 * <p>路径前缀 {@code auth/sms} 配合网关的 {@code Path=/api/auth/**} 路由
 * （重写规则会把 {@code /api} 去掉，所以前端写 {@code /api/auth/sms/sendCode}，到这里就是 {@code /auth/sms/sendCode}）。</p>
 */
@Slf4j
@RestController
@RequestMapping("auth/sms")
public class SmsAuthController {

    private final ThirdPartFeignService thirdPartFeignService;
    private final MemberFeignService memberFeignService;
    private final StringRedisTemplate stringRedisTemplate;

    public SmsAuthController(ThirdPartFeignService thirdPartFeignService,
                             MemberFeignService memberFeignService,
                             StringRedisTemplate stringRedisTemplate) {
        this.thirdPartFeignService = thirdPartFeignService;
        this.memberFeignService = memberFeignService;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 发送短信验证码。
     *
     * <p>注意 third-party 服务里也有一个同名接口（{@code /thirdParty/sms/sendCode}），别调错：
     * 前端要调的是这个（走 {@code /api/auth/sms/sendCode}），它负责防刷和验证码的存取，
     * third-party 那个只负责真正把短信发出去。</p>
     */
    @GetMapping("/sendCode")
    public R sendCode(@RequestParam("phone") String phone) {
        log.info("发送验证码: {}", phone);
        int time = 5;

        //1、接口防刷：同一个手机号 60 秒内只能发一次（防刷窗口和验证码共用 sms:code:<phone> 这个 key）
        if (VerifyCodeUtils.remainingSeconds(stringRedisTemplate, AuthServerConstant.SMS_CODE_CACHE_PREFIX, phone) > 0) {
            return R.error(BaseCodeEnum.SMS_CODE_EXCEPTION.getCode(), BaseCodeEnum.SMS_CODE_EXCEPTION.getMsg());
        }

        //2、生成验证码、存 Redis（time 分钟），再交给 third-party 真正把短信发出去
        VerifyCodeUtils.send(stringRedisTemplate, AuthServerConstant.SMS_CODE_CACHE_PREFIX, phone, time, codeNum -> {
            R r = thirdPartFeignService.sendCode(phone, codeNum, time);
            if (r.getCode() != 0) {
                throw new BaseException("远程服务调用失败" + r.getMsg());
            }
        });

        return R.ok();
    }

    /**
     * 用户注册。
     *
     * <p>校验分两层，但返回的是同一套结构，前端只要认 {@code errors} 这一个地方：</p>
     * <ul>
     *   <li>字段本身不合法（用户名长度、手机号格式、没勾协议……）→ {@code @Valid} 抛
     *       MethodArgumentNotValidException，由 common 的 GlobalExceptionHandler 统一转成
     *       {@code code:10001 + errors{字段:消息}}，这里不用写一行代码。
     *       特意<b>不</b>接 BindingResult，接了就没有异常，也就享受不到那个统一处理了。</li>
     *   <li>跨字段的规则（两次密码不一致、验证码不对）bean validation 管不了，在这里手工返回同样的结构。</li>
     * </ul>
     */
    @PostMapping("/register")
    public R register(@RequestBody @Valid UserRegisterVo vo) {
        // 不要用 JSON.toJSONString(vo) 打日志：那会把明文密码写进日志文件
        log.info("用户注册: userName={}, phone={}", vo.getUserName(), vo.getPhone());

        // 跨字段校验：两次密码要一致。
        // 用 Objects.equals 而不是 vo.getPassword().equals(...) —— JSON 请求里漏传 password 时它是 null，
        // 原来的写法在这种情况会 NPE（空表单提交时字段是 ""，所以只有换成 JSON 之后才会暴露）
        if (!Objects.equals(vo.getPassword(), vo.getPassword2())) {
            return fieldError("password2", "两次输入的密码不一致");
        }

        //1、校验验证码（只读不写，等业务成功之后才决定删不删）
        if (!VerifyCodeUtils.verify(stringRedisTemplate, AuthServerConstant.SMS_CODE_CACHE_PREFIX,
                vo.getPhone(), vo.getCode())) {
            return fieldError("code", "验证码错误");
        }

        //2、真正注册，调 member 服务
        R register = memberFeignService.register(vo);
        if (register.getCode() != 0) {
            // member 那边用 15001/15002 区分是用户名重复还是手机号重复，msg 已经是给人看的中文，直接透传
            //
            // 这里故意【不】删验证码（原来 delete 写在这一步之前，会把验证码消耗掉）：
            // 用户名/手机号撞了属于业务失败，用户改个名字就该能用同一个验证码重试，
            // 不该被迫再发一次短信。不删还有个附带好处 —— 防刷是靠 sms:code:<phone> 这个 key
            // 是否存在来判断的，保留它意味着失败的注册不会把 60 秒防刷窗口重置掉。
            // 安全性没有变弱：能走到这一步说明调用方已经知道正确的验证码了。
            log.warn("注册失败: code={}, msg={}", register.getCode(), register.getMsg());
            return R.error(register.getCode(), register.getMsg());
        }

        //3、注册成功才删掉验证码（令牌机制：一次性，用过即废）
        VerifyCodeUtils.consume(stringRedisTemplate, AuthServerConstant.SMS_CODE_CACHE_PREFIX, vo.getPhone());

        return R.ok();
    }

    /** 拼一个和 GlobalExceptionHandler 一模一样的校验错误结构，免得前端要认两种格式 */
    private R fieldError(String field, String message) {
        Map<String, String> errors = new HashMap<>();
        errors.put(field, message);
        return R.error(BaseCodeEnum.VALID_EXCEPTION.getCode(), BaseCodeEnum.VALID_EXCEPTION.getMsg())
                .put("errors", errors);
    }
}
