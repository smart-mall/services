package auth.controller;

import auth.feign.MemberFeignService;
import auth.utils.VerifyCodeUtils;
import auth.vo.EmailChangeVo;
import auth.vo.MobileChangeVo;
import com.alibaba.fastjson.TypeReference;
import common.constant.AuthServerConstant;
import common.exception.BaseCodeEnum;
import common.exception.ValidationException;
import common.utils.JwtUtils;
import common.utils.LoginUserUtils;
import common.utils.R;
import common.vo.MemberResponseVo;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

/**
 * 当前用户的账号信息：读完整资料、换绑手机号 / 邮箱。
 *
 * <p><b>这里自己验签，不走网关注入的 {@code X-Member-Claims}</b>。
 * 其余服务是信任那个头的（网关会把客户端伪造的同名头抹掉），但前提是请求必须经过网关 ——
 * 而 {@code gl-com.yml} 把每个服务的端口都映射到了宿主机，直连 53100 就能自己塞一个
 * {@code X-Member-Claims} 冒充任意用户。这几个接口自己验 JWT 签名，所以直连也安全。</p>
 *
 * <p>代价是 token 会被验两遍（网关一次、这里一次）。这是有意的：
 * 只有拿到"完整用户信息"和"改登录标识"这类敏感操作才需要独立可信的身份来源，
 * 商品、购物车那些接口没必要为它付出这个成本。</p>
 *
 * <p><b>换绑为什么放在 auth 而不是 member</b>：手机号和邮箱是登录标识，验证码的生成、
 * 防刷、一次性消费都收在 auth 的 {@link VerifyCodeUtils} 里（Redis key 的格式只有它知道）。
 * member 那边只提供两个不含校验的服务间接口，查重由它做（表是它的）。</p>
 *
 * <p><b>未登录为什么用 {@code writeUnauthorized} 而不是 {@code ResponseEntity}</b>：
 * 401 的响应体（状态码 + {@code {code,msg}} + charset）是前端契约，应该只有一个产出点。
 * {@link LoginUserUtils#writeUnauthorized} 就是那一处，拦截器也用同一个，
 * 所以这里跟它们保持一致，而不是自己拼一个长得差不多的。</p>
 */
@Slf4j
@RestController
@RequestMapping("auth/user")
public class UserController {

    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer";

    private final MemberFeignService memberFeignService;
    private final JwtUtils jwtUtils;
    private final StringRedisTemplate stringRedisTemplate;

    public UserController(MemberFeignService memberFeignService,
                          JwtUtils jwtUtils,
                          StringRedisTemplate stringRedisTemplate) {
        this.memberFeignService = memberFeignService;
        this.jwtUtils = jwtUtils;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 当前登录用户的完整信息。
     *
     * <p>token 从 {@code Authorization: Bearer xxx} 取（前端 request.ts 就是这么发的），
     * 也容忍只写 token 不带前缀。</p>
     *
     * <p>未登录 / token 无效 → <b>HTTP 401</b>；前端 request.ts 是看状态码 401 去清
     * localStorage 里的 token 的，所以必须是真 401，不能塞在 body 的 code 里。</p>
     */
    @GetMapping("/info")
    public R info(@RequestHeader(value = AUTH_HEADER, required = false) String authorization,
                  HttpServletResponse response) throws IOException {

        TokenCheck check = checkToken(authorization);
        if (!check.isOk()) {
            return unauthorized(response, check.failure());
        }

        R memberR = memberFeignService.getUserInfo(check.memberId());
        if (memberR.getCode() != 0) {
            log.warn("查询用户信息失败: memberId={}, code={}, msg={}",
                    check.memberId(), memberR.getCode(), memberR.getMsg());
            return R.error(memberR.getCode(), memberR.getMsg());
        }

        // 注意键是 member 不是 data：member 的 /info/{id} 是代码生成器产出的 R.ok().put("member", ...)
        MemberResponseVo user = memberR.getData("member", new TypeReference<MemberResponseVo>() {});
        if (user == null) {
            // 验签过了但库里没人：账号被删了，等同于未登录
            log.warn("token 有效但会员不存在: memberId={}", check.memberId());
            return unauthorized(response, BaseCodeEnum.NOT_LOGIN_EXCEPTION);
        }

        // 和 AbstractLoginController#issueToken 一样：这两个字段不能出这个接口
        user.setPassword(null);
        user.setAccessToken(null);

        return R.ok().setData(user);
    }

    /**
     * 换绑手机号。
     *
     * <p>只接受新号码和验证码，改的是哪个账号由 token 决定。号码已被别人绑定时
     * member 返回 15006，这里原样透传。</p>
     *
     * <p><b>验证码只读不删，等换绑成功才消费</b>：反过来的话，member 因为号码被占用而拒绝时，
     * 用户没法拿同一个码换个号码重试，还得等 60 秒防刷窗口过去。</p>
     */
    @PutMapping("/mobile")
    public R changeMobile(@Valid @RequestBody MobileChangeVo vo,
                          @RequestHeader(value = AUTH_HEADER, required = false) String authorization,
                          HttpServletResponse response) throws IOException {

        TokenCheck check = checkToken(authorization);
        if (!check.isOk()) {
            return unauthorized(response, check.failure());
        }

        if (!VerifyCodeUtils.verify(stringRedisTemplate, AuthServerConstant.SMS_CODE_CACHE_PREFIX,
                vo.getMobile(), vo.getCode())) {
            throw new ValidationException("code", "验证码错误");
        }

        R r = memberFeignService.changeMobile(vo.getMobile());
        if (r.getCode() != 0) {
            return R.error(r.getCode(), r.getMsg());
        }

        VerifyCodeUtils.consume(stringRedisTemplate, AuthServerConstant.SMS_CODE_CACHE_PREFIX, vo.getMobile());
        return R.ok();
    }

    /** 换绑邮箱，语义同 {@link #changeMobile}，占用时返回 15007 */
    @PutMapping("/email")
    public R changeEmail(@Valid @RequestBody EmailChangeVo vo,
                         @RequestHeader(value = AUTH_HEADER, required = false) String authorization,
                         HttpServletResponse response) throws IOException {

        TokenCheck check = checkToken(authorization);
        if (!check.isOk()) {
            return unauthorized(response, check.failure());
        }

        if (!VerifyCodeUtils.verify(stringRedisTemplate, AuthServerConstant.EMAIL_CODE_CACHE_PREFIX,
                vo.getEmail(), vo.getCode())) {
            throw new ValidationException("code", "验证码错误");
        }

        R r = memberFeignService.changeEmail(vo.getEmail());
        if (r.getCode() != 0) {
            return R.error(r.getCode(), r.getMsg());
        }

        VerifyCodeUtils.consume(stringRedisTemplate, AuthServerConstant.EMAIL_CODE_CACHE_PREFIX, vo.getEmail());
        return R.ok();
    }

    /**
     * 验签并取出会员 id。
     *
     * <p>失败时不直接返回响应，而是把该用的错误码带回来由调用方转 —— 这样三个接口共用
     * 同一段验签，401 的文案也不会各写一份。</p>
     */
    private TokenCheck checkToken(String authorization) {

        String token = resolveToken(authorization);
        if (token == null) {
            return TokenCheck.fail(BaseCodeEnum.NOT_LOGIN_EXCEPTION);
        }

        MemberResponseVo claims;
        try {
            // 验签 + 顺带校验 exp
            claims = jwtUtils.parse(token);
        } catch (ExpiredJwtException e) {
            // 过期单独区分出来，前端可以提示"登录已过期"而不是"请先登录"
            log.debug("token 已过期: {}", e.getMessage());
            return TokenCheck.fail(BaseCodeEnum.LOGIN_EXPIRED_EXCEPTION);
        } catch (JwtException | IllegalArgumentException e) {
            // 签名不对 / 格式错 / 算法被改 / token 为空
            log.debug("token 无效: {} - {}", e.getClass().getSimpleName(), e.getMessage());
            return TokenCheck.fail(BaseCodeEnum.NOT_LOGIN_EXCEPTION);
        }

        if (claims.getId() == null) {
            log.warn("token 验签通过但没有 uid，不可能是本服务签发的: {}", claims);
            return TokenCheck.fail(BaseCodeEnum.NOT_LOGIN_EXCEPTION);
        }

        return TokenCheck.ok(claims.getId());
    }

    /** 验签结果：成功时 memberId 非空，失败时 failure 是调用方该返回的错误码 */
    private record TokenCheck(Long memberId, BaseCodeEnum failure) {

        static TokenCheck ok(Long memberId) {
            return new TokenCheck(memberId, null);
        }

        static TokenCheck fail(BaseCodeEnum failure) {
            return new TokenCheck(null, failure);
        }

        boolean isOk() {
            return memberId != null;
        }
    }

    /**
     * 从 Authorization 头里取 token，容忍 {@code Bearer xxx} 和只写 token 两种写法，
     * 前缀大小写也认。和网关的 JwtAuthFilter#resolveToken 保持一致。
     */
    private String resolveToken(String header) {
        if (header == null || header.isBlank()) {
            return null;
        }
        String value = header.trim();
        if (value.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            value = value.substring(BEARER_PREFIX.length()).trim();
        }
        return value.isEmpty() ? null : value;
    }

    /** 写好 401 + JSON 并返回 null（Spring 对 null 返回不再写 body，状态码和内容保持不变） */
    private R unauthorized(HttpServletResponse response, BaseCodeEnum codeEnum) throws IOException {
        log.debug("未登录或 token 无效: {}", codeEnum.getMsg());
        LoginUserUtils.writeUnauthorized(response, codeEnum);
        return null;
    }
}
