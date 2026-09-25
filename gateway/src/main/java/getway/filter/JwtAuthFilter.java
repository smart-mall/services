package getway.filter;

import common.utils.JwtUtils;
import common.utils.LoginUserUtils;
import common.vo.MemberResponseVo;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import static common.constant.AuthServerConstant.CLIENT_IP_HEADER;
import static common.constant.AuthServerConstant.MEMBER_CLAIMS_HEADER;

/**
 * 网关统一验签。
 *
 * <p>登录态从 Session 换成 JWT 之后的职责划分：<b>只有网关解析 JWT</b>。
 * 验签通过就把用户信息塞进 {@link common.constant.AuthServerConstant#MEMBER_CLAIMS_HEADER}
 * 请求头往下一层传，业务服务只认这个头、不再解析 JWT，所以 order/cart/seckill
 * 不需要引 jjwt，也不需要知道 token 长什么样。</p>
 *
 * <p><b>几个刻意的设计决定</b>：</p>
 * <ul>
 *   <li><b>没有 token 就是匿名，直接放行。</b>商品、搜索这些页面本来就不需要登录，
 *       网关不做"一刀切要求登录"，谁需要登录由谁自己判断。</li>
 *   <li><b>token 无效（过期/伪造/格式错）也按匿名放行</b>，不在这里返回 401。
 *       因为 localStorage 里的 token 过期后，SPA 很可能正好在浏览商品列表 ——
 *       网关一 401，整个首页会一起报错。放行之后：公开接口照常工作，
 *       需要登录的接口（如 {@code /api/auth/userinfo}）由服务自己返回 401，
 *       前端拿到 401 清掉 token 并跳登录页，下一个请求就恢复正常了。</li>
 *   <li><b>必须把客户端自己传的 X-Member-Claims 抹掉</b>。否则任何人手写一个
 *       {@code X-Member-Claims: base64(...)} 就能冒充任意用户。
 *       注意：业务服务都是直接对外暴露端口的（见 gl-com.yml 的 ports），
 *       绕过网关直接打 52400 / 53100 依然能伪造这个头 —— 真要堵住得让每个服务自己验签，
 *       本项目的假设是"服务在内网、只经网关访问"。</li>
 *   <li><b>顺带注入 X-Client-IP</b>，同样抹掉客户端传的同名头。网关是边缘，
 *       只有它拿得到真实客户端地址；下游的 remoteAddr 是网关自己。</li>
 * </ul>
 */
@Slf4j
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer";

    private final JwtUtils jwtUtils;

    public JwtAuthFilter(JwtUtils jwtUtils) {
        this.jwtUtils = jwtUtils;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = resolveToken(request);
        // 只认 remoteAddr：网关就是边缘，前面没有可信代理。信任 X-Forwarded-For
        // 等于让调用方随便伪造 IP（旁边 X-Member-Claims 就是同一个坑）
        String clientIp = request.getRemoteAddr();

        if (token == null) {
            // 没带 token：纯匿名请求，把客户端可能伪造的 claims 头抹掉后放行
            filterChain.doFilter(new TrustedHeadersRequest(request, null, clientIp), response);
            return;
        }

        try {
            MemberResponseVo user = jwtUtils.parse(token);
            filterChain.doFilter(new TrustedHeadersRequest(request, LoginUserUtils.encode(user), clientIp), response);
        } catch (JwtException | IllegalArgumentException e) {
            // 过期 / 签名不对 / 格式错 / 算法被改，全部按未登录处理。
            // 只打 debug：伪造 token 的扫描请求会很吵，不值得刷 warn
            log.debug("token 无效，按匿名请求处理: {} - {}", e.getClass().getSimpleName(), e.getMessage());
            filterChain.doFilter(new TrustedHeadersRequest(request, null, clientIp), response);
        }
    }

    /**
     * 从 Authorization 头里取 token。
     *
     * <p>容忍两种写法：标准的 {@code Bearer xxx}，以及前端图省事只写了 token。
     * 前缀大小写也认（{@code bearer} / {@code Bearer}）。</p>
     */
    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader(AUTH_HEADER);
        if (header == null || header.isBlank()) {
            return null;
        }
        String value = header.trim();
        if (value.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            value = value.substring(BEARER_PREFIX.length()).trim();
        }
        return value.isEmpty() ? null : value;
    }

    /**
     * 只暴露网关自己产出的 {@link common.constant.AuthServerConstant#MEMBER_CLAIMS_HEADER}
     * 与 {@link common.constant.AuthServerConstant#CLIENT_IP_HEADER}，
     * 客户端传的同名头一律不认。
     *
     * <p>三个方法都要覆盖：{@code getHeader} 管业务代码的读取，
     * {@code getHeaders} / {@code getHeaderNames} 管网关转发时重建请求头的过程。
     * 只覆盖第一个的话，伪造的头还是会被原样转发给下游。</p>
     */
    private static class TrustedHeadersRequest extends HttpServletRequestWrapper {

        private final String claims;
        private final String clientIp;

        TrustedHeadersRequest(HttpServletRequest request, String claims, String clientIp) {
            super(request);
            this.claims = claims;
            this.clientIp = clientIp;
        }

        @Override
        public String getHeader(String name) {
            if (isTrusted(name)) {
                return trustedValue(name);
            }
            return super.getHeader(name);
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            if (isTrusted(name)) {
                String value = trustedValue(name);
                return value == null
                        ? Collections.emptyEnumeration()
                        : Collections.enumeration(List.of(value));
            }
            return super.getHeaders(name);
        }

        @Override
        public Enumeration<String> getHeaderNames() {
            List<String> names = new ArrayList<>();
            Enumeration<String> origin = super.getHeaderNames();
            while (origin != null && origin.hasMoreElements()) {
                String name = origin.nextElement();
                if (!isTrusted(name)) {
                    names.add(name);
                }
            }
            if (claims != null) {
                names.add(MEMBER_CLAIMS_HEADER);
            }
            if (clientIp != null) {
                names.add(CLIENT_IP_HEADER);
            }
            return Collections.enumeration(names);
        }

        private boolean isTrusted(String name) {
            return MEMBER_CLAIMS_HEADER.equalsIgnoreCase(name) || CLIENT_IP_HEADER.equalsIgnoreCase(name);
        }

        private String trustedValue(String name) {
            return MEMBER_CLAIMS_HEADER.equalsIgnoreCase(name) ? claims : clientIp;
        }
    }
}
