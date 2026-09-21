package auth.controller;

import auth.feign.MemberFeignService;
import com.alibaba.fastjson.TypeReference;
import common.exception.BaseCodeEnum;
import common.utils.JwtUtils;
import common.utils.R;
import common.vo.MemberResponseVo;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户信息。
 *
 * <p><b>这里自己验签，不走网关注入的 {@code X-Member-Claims}</b>。
 * 其余服务是信任那个头的（网关会把客户端伪造的同名头抹掉），但前提是请求必须经过网关 ——
 * 而 {@code gl-com.yml} 把每个服务的端口都映射到了宿主机，直连 53100 就能自己塞一个
 * {@code X-Member-Claims} 冒充任意用户。这个接口自己验 JWT 签名，所以直连也安全。</p>
 *
 * <p>代价是 token 会被验两遍（网关一次、这里一次）。这是有意的：
 * 只有拿到"完整用户信息"这种敏感操作才需要独立可信的身份来源，
 * 商品、购物车那些接口没必要为它付出这个成本。</p>
 *
 * <p><b>为什么要回查数据库</b>：JWT 里只放了 uid/username/nickname/header/integration
 * （见 {@link JwtUtils#create}），而"完整信息"里的生日、城市、职业、签名、会员等级、成长值
 * 都不在 token 里。顺带一个副作用：token 里的昵称/头像会过期（用户改了头像、token 里还是旧的），
 * 所以前端应该以这个接口的返回为准。</p>
 */
@Slf4j
@RestController
@RequestMapping("auth/user")
public class UserController {

    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer";

    private final MemberFeignService memberFeignService;
    private final JwtUtils jwtUtils;

    public UserController(MemberFeignService memberFeignService, JwtUtils jwtUtils) {
        this.memberFeignService = memberFeignService;
        this.jwtUtils = jwtUtils;
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
    public ResponseEntity<R> info(@RequestHeader(value = AUTH_HEADER, required = false) String authorization) {

        String token = resolveToken(authorization);
        if (token == null) {
            return unauthorized(BaseCodeEnum.NOT_LOGIN_EXCEPTION);
        }

        MemberResponseVo claims;
        try {
            // 验签 + 顺带校验 exp
            claims = jwtUtils.parse(token);
        } catch (ExpiredJwtException e) {
            // 过期单独区分出来，前端可以提示"登录已过期"而不是"请先登录"
            log.debug("token 已过期: {}", e.getMessage());
            return unauthorized(BaseCodeEnum.LOGIN_EXPIRED_EXCEPTION);
        } catch (JwtException | IllegalArgumentException e) {
            // 签名不对 / 格式错 / 算法被改 / token 为空
            log.debug("token 无效: {} - {}", e.getClass().getSimpleName(), e.getMessage());
            return unauthorized(BaseCodeEnum.NOT_LOGIN_EXCEPTION);
        }

        if (claims.getId() == null) {
            log.warn("token 验签通过但没有 uid，不可能是本服务签发的: {}", claims);
            return unauthorized(BaseCodeEnum.NOT_LOGIN_EXCEPTION);
        }

        R memberR = memberFeignService.getUserInfo(claims.getId());
        if (memberR.getCode() != 0) {
            log.warn("查询用户信息失败: memberId={}, code={}, msg={}",
                    claims.getId(), memberR.getCode(), memberR.getMsg());
            return ResponseEntity.ok(R.error(memberR.getCode(), memberR.getMsg()));
        }

        // 注意键是 member 不是 data：member 的 /info/{id} 是代码生成器产出的 R.ok().put("member", ...)
        MemberResponseVo user = memberR.getData("member", new TypeReference<MemberResponseVo>() {});
        if (user == null) {
            // 验签过了但库里没人：账号被删了，等同于未登录
            log.warn("token 有效但会员不存在: memberId={}", claims.getId());
            return unauthorized(BaseCodeEnum.NOT_LOGIN_EXCEPTION);
        }

        // 和 AbstractLoginController#issueToken 一样：这两个字段不能出这个接口
        user.setPassword(null);
        user.setAccessToken(null);

        return ResponseEntity.ok(R.ok().setData(user));
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

    private ResponseEntity<R> unauthorized(BaseCodeEnum codeEnum) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(R.error(codeEnum.getCode(), codeEnum.getMsg()));
    }
}
