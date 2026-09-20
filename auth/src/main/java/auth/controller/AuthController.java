package auth.controller;

import auth.feign.MemberFeignService;
import auth.feign.ThirdPartFeignService;
import auth.vo.UserLoginVo;
import auth.vo.UserRegisterVo;
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
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * 前台认证接口（Vue SPA 用）。
 *
 * <p>这个类取代了原来的 {@code LoginController}。原来那套是给 Thymeleaf 服务端渲染用的：
 * 返回视图名（login / reg / logout）或者 {@code redirect:http://auth.gulimall.com/xxx.html}，
 * 校验失败靠 {@code RedirectAttributes} 把错误塞进 flash 再 302 回注册页。SPA 吃不了这一套，
 * 所以这里全部改成返回 {@link R} 的 JSON，登录态用 JWT 而不是 HttpSession。</p>
 *
 * <p>路径前缀 {@code auth} 是为了配合网关的 {@code Path=/api/auth/**} 路由
 * （重写规则会把 {@code /api} 去掉，所以前端写 {@code /api/auth/login}，到这里就是 {@code /auth/login}）。</p>
 *
 * <p>错误约定：</p>
 * <ul>
 *   <li>参数校验失败 → HTTP 200 + {@code code:10001} + {@code errors{字段:消息}}（由 GlobalExceptionHandler 统一产出）</li>
 *   <li>业务失败 → HTTP 200 + member 服务给的 code（如 15001/15002/15003）</li>
 *   <li>未登录 → <b>HTTP 401</b>。前端 request.ts 是看 HTTP 状态码 401 去清 localStorage 里的 token 的，
 *       所以这个必须是真的 401，不能塞在 code 里</li>
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping("auth")
public class AuthController {

    private final ThirdPartFeignService thirdPartFeignService;
    private final MemberFeignService memberFeignService;
    private final StringRedisTemplate stringRedisTemplate;
    private final JwtUtils jwtUtils;

    public AuthController(ThirdPartFeignService thirdPartFeignService,
                          MemberFeignService memberFeignService,
                          StringRedisTemplate stringRedisTemplate,
                          JwtUtils jwtUtils) {
        this.thirdPartFeignService = thirdPartFeignService;
        this.memberFeignService = memberFeignService;
        this.stringRedisTemplate = stringRedisTemplate;
        this.jwtUtils = jwtUtils;
    }

    /**
     * 发送短信验证码。
     *
     * <p>原来的路径是 {@code /sms/sendCode}，现在挂在 {@code auth} 下面。
     * 注意 third-party 服务里也有一个同名接口（{@code /sms/sendCode}），别调错：
     * 前端要调的是这个（走 {@code /api/auth/sms/sendCode}），它负责防刷和验证码的存取，
     * third-party 那个只负责真正把短信发出去。</p>
     */
    @GetMapping("/sms/sendCode")
    public R sendCode(@RequestParam("phone") String phone) {
        log.info("发送验证码: {}", phone);
        int time = 5;

        //1、接口防刷
        String redisCode = stringRedisTemplate.opsForValue().get(AuthServerConstant.SMS_CODE_CACHE_PREFIX + phone);
        if (!StringUtils.isEmpty(redisCode)) {
            //活动存入redis的时间，用当前时间减去存入redis的时间，判断用户手机号是否在60s内发送验证码
            long currentTime = Long.parseLong(redisCode.split("_")[1]);
            if (System.currentTimeMillis() - currentTime < 60000) {
                //60s内不能再发
                return R.error(BaseCodeEnum.SMS_CODE_EXCEPTION.getCode(), BaseCodeEnum.SMS_CODE_EXCEPTION.getMsg());
            }
        }

        //2、生成验证码 redis.存key-phone,value-code_时间戳
        int code = (int) ((Math.random() * 9 + 1) * 100000);
        String codeNum = String.valueOf(code);
        String redisStorage = codeNum + "_" + System.currentTimeMillis();

        stringRedisTemplate.opsForValue().set(AuthServerConstant.SMS_CODE_CACHE_PREFIX + phone,
                redisStorage, time, TimeUnit.MINUTES);

        R r = thirdPartFeignService.sendCode(phone, codeNum, time);
        if (r.getCode() != 0) {
            throw new BaseException("远程服务调用失败" + r.getMsg());
        }

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

        //1、校验验证码
        String redisCode = stringRedisTemplate.opsForValue()
                .get(AuthServerConstant.SMS_CODE_CACHE_PREFIX + vo.getPhone());
        if (StringUtils.isEmpty(redisCode) || !vo.getCode().equals(redisCode.split("_")[0])) {
            return fieldError("code", "验证码错误");
        }

        //2、验证码通过，删掉它（令牌机制：一次性，用过即废）
        stringRedisTemplate.delete(AuthServerConstant.SMS_CODE_CACHE_PREFIX + vo.getPhone());

        //3、真正注册，调 member 服务
        R register = memberFeignService.register(vo);
        if (register.getCode() != 0) {
            // member 那边用 15001/15002 区分是用户名重复还是手机号重复，msg 已经是给人看的中文，直接透传
            log.warn("注册失败: code={}, msg={}", register.getCode(), register.getMsg());
            return R.error(register.getCode(), register.getMsg());
        }

        return R.ok();
    }

    /**
     * 用户登录，成功后签发 JWT。
     *
     * <p>返回结构：{@code {code:0, msg:"success", data:{token, expiresIn, user}}}。</p>
     *
     * <p>为什么是 data 里再套一层而不是平铺：前端 request.ts 里写的取值习惯是
     * {@code res.data.data}（对应后端 {@code R.ok().setData(x)}），所以 token 放 data 里最省事。</p>
     */
    @PostMapping("/login")
    public R login(@RequestBody @Valid UserLoginVo vo) {
        // 同样不打明文密码
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

    /** 拼一个和 GlobalExceptionHandler 一模一样的校验错误结构，免得前端要认两种格式 */
    private R fieldError(String field, String message) {
        Map<String, String> errors = new HashMap<>();
        errors.put(field, message);
        return R.error(BaseCodeEnum.VALID_EXCEPTION.getCode(), BaseCodeEnum.VALID_EXCEPTION.getMsg())
                .put("errors", errors);
    }

    private ResponseEntity<R> unauthorized(BaseCodeEnum codeEnum) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(R.error(codeEnum.getCode(), codeEnum.getMsg()));
    }
}
