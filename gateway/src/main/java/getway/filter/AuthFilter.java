package getway.filter;

import com.alibaba.fastjson.JSON;
import common.exception.BaseCodeEnum;
import common.utils.JwtUtils;
import common.utils.LoginUserUtils;
import common.utils.R;
import common.vo.AdminResponseVo;
import common.vo.MemberResponseVo;
import getway.config.AuthRuleProperties;
import getway.feign.AdminAuthFeignService;
import getway.vo.AdminVerifyVo;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static common.constant.AuthServerConstant.ADMIN_HEADER;
import static common.constant.AuthServerConstant.CLIENT_IP_HEADER;
import static common.constant.AuthServerConstant.MEMBER_CLAIMS_HEADER;

/**
 * 网关统一鉴权：{@code /api/{模块}/front/jwt/**} 要会员凭证，{@code /api/{模块}/front/**} 公开，
 * 其余 {@code /api/**} 要管理端凭证，校验通过后把身份写进请求头往下传。
 *
 * <p>客户端自带的同名身份头会被抹掉，下游只信任网关写入的值。
 */
@Slf4j
@Component
public class AuthFilter extends OncePerRequestFilter {

    /** 鉴权规则只作用于该前缀下的请求。 */
    private static final String API_PREFIX = "/api";

    /** 管理端 token 的请求头名，必须与 renren-fast 读取的一致。 */
    private static final String ADMIN_TOKEN_HEADER = "token";

    /** 必须先于 {@link #FRONT} 判定：它是后者的子集。 */
    private static final Pattern FRONT_JWT = Pattern.compile("^/api/[^/]+/front/jwt(/.*)?$");

    /** 前台接口路径规则：{@code /api/{模块}/front/**}。 */
    private static final Pattern FRONT = Pattern.compile("^/api/[^/]+/front(/.*)?$");

    private final AuthRuleProperties properties;

    private final JwtUtils jwtUtils;

    private final AdminAuthFeignService adminAuthFeignService;

    private final AntPathMatcher antPathMatcher = new AntPathMatcher();

    /**
     * 创建网关鉴权过滤器。
     *
     * @param properties 匿名路径白名单配置
     * @param jwtUtils 会员凭证验签工具
     * @param adminAuthFeignService 管理端 token 校验的远程调用接口
     */
    public AuthFilter(AuthRuleProperties properties,
                      JwtUtils jwtUtils,
                      AdminAuthFeignService adminAuthFeignService) {
        this.properties = properties;
        this.jwtUtils = jwtUtils;
        this.adminAuthFeignService = adminAuthFeignService;
    }

    /**
     * {@inheritDoc}
     *
     * <p>OPTIONS 预检与匿名白名单直接放行，其余请求按前台、后台规则分别校验，不通过时写好响应即返回。
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String uri = request.getRequestURI();

        // 要注入给下游的身份头，缺项表示这个头不下发
        Map<String, String> identity = new LinkedHashMap<>();
        // 只认 remoteAddr：前面没有可信代理，X-Forwarded-For 是调用方随便写的
        identity.put(CLIENT_IP_HEADER, request.getRemoteAddr());

        // 预检不带凭证，拦了等于废掉所有带自定义头的跨源请求
        if (!HttpMethod.OPTIONS.matches(request.getMethod()) && !isAnonymous(uri)) {
            if (FRONT_JWT.matcher(uri).matches()) {
                if (!verifyMember(request, response, identity)) {
                    return;
                }
            } else if (isAdminPath(uri)) {
                if (!verifyAdmin(request, response, identity)) {
                    return;
                }
            }
        }

        filterChain.doFilter(new TrustedHeadersRequest(request, identity), response);
    }

    /** 后台接口：{@code /api/**} 里除前台公开接口之外的那些。 */
    private boolean isAdminPath(String uri) {
        boolean api = uri.equals(API_PREFIX) || uri.startsWith(API_PREFIX + "/");
        return api && !FRONT.matcher(uri).matches();
    }

    private boolean isAnonymous(String uri) {
        for (String pattern : properties.getAnonymousPaths()) {
            if (antPathMatcher.match(pattern, uri)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 校验会员凭证：网关与 auth 共用同一份密钥，本地验签，不调用其他服务。
     *
     * @param request 当前请求，从 {@code Authorization} 头取 Bearer token
     * @param response 校验不通过时写入错误响应
     * @param identity 身份头容器，校验通过时写入会员信息
     * @return 是否放行；不通过时已经写好响应
     * @throws IOException 写响应体失败时抛出
     */
    private boolean verifyMember(HttpServletRequest request, HttpServletResponse response,
                                 Map<String, String> identity) throws IOException {
        String token = LoginUserUtils.resolveBearer(request.getHeader(HttpHeaders.AUTHORIZATION));
        if (token == null) {
            return fail(response, BaseCodeEnum.NOT_LOGIN_EXCEPTION);
        }

        MemberResponseVo user;
        try {
            // 验签与 exp 校验都在 parse 内部完成，这里只按异常类型分流
            user = jwtUtils.parse(token);
        } catch (ExpiredJwtException e) {
            // 过期单独给 15005，前端据此提示"登录已过期"而不是"请先登录"
            return fail(response, BaseCodeEnum.LOGIN_EXPIRED_EXCEPTION);
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("token 无效: {} - {}", e.getClass().getSimpleName(), e.getMessage());
            return fail(response, BaseCodeEnum.NOT_LOGIN_EXCEPTION);
        }

        if (user.getId() == null) {
            log.warn("token 验签通过但没有 uid");
            return fail(response, BaseCodeEnum.NOT_LOGIN_EXCEPTION);
        }

        identity.put(MEMBER_CLAIMS_HEADER, LoginUserUtils.encode(user));
        return true;
    }

    /**
     * 校验管理端凭证：token 是不透明串、只存在 renren 的库里，只能委托 {@link AdminAuthFeignService} 校验。
     *
     * @param request 当前请求，token 可能放在请求头或查询参数里
     * @param response 校验不通过时写入错误响应
     * @param identity 身份头容器，校验通过时写入管理员信息
     * @return 是否放行；不通过时已经写好响应
     * @throws IOException 写响应体失败时抛出
     */
    private boolean verifyAdmin(HttpServletRequest request, HttpServletResponse response,
                                Map<String, String> identity) throws IOException {
        String token = resolveAdminToken(request);
        if (token == null) {
            return fail(response, BaseCodeEnum.ADMIN_NOT_LOGIN_EXCEPTION);
        }

        R<AdminVerifyVo> verified;
        try {
            verified = adminAuthFeignService.verify(token);
        } catch (Exception e) {
            log.warn("管理端凭证校验调用失败: {}", e.getMessage());
            return fail(response, BaseCodeEnum.AUTH_UNAVAILABLE);
        }

        AdminVerifyVo adminInfo = verified == null ? null : verified.getData();
        if (adminInfo == null || adminInfo.getUserId() == null) {
            return fail(response, BaseCodeEnum.ADMIN_NOT_LOGIN_EXCEPTION);
        }

        AdminResponseVo admin = new AdminResponseVo();
        admin.setId(adminInfo.getUserId());
        admin.setUsername(adminInfo.getUsername());

        identity.put(ADMIN_HEADER, LoginUserUtils.encode(admin));
        return true;
    }

    /** 管理端 token 也可能放在查询参数里：OSS 上传是浏览器直连的 el-upload，只能把 token 拼在 URL 上。 */
    private String resolveAdminToken(HttpServletRequest request) {
        String token = request.getHeader(ADMIN_TOKEN_HEADER);
        if (token == null || token.isBlank()) {
            token = request.getParameter(ADMIN_TOKEN_HEADER);
        }
        return token == null || token.isBlank() ? null : token.trim();
    }

    /** 校验不通过：写好响应并返回 false，调用方直接结束。 */
    private boolean fail(HttpServletResponse response, BaseCodeEnum codeEnum) throws IOException {
        write(response, codeEnum.getCode(), codeEnum.getMsg());
        return false;
    }

    /** 拒绝与报错统一走这里：状态码恒为 200，结果看 body 的 code。 */
    private void write(HttpServletResponse response, int code, String msg) throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json;charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(JSON.toJSONString(R.error(code, msg)));
    }

    /** 请求包装器：只暴露网关自己产出的身份头，客户端传的同名头一律抹掉。 */
    private static class TrustedHeadersRequest extends HttpServletRequestWrapper {

        /** 网关产出的身份头名，其余请求头原样透传。 */
        private static final List<String> TRUSTED = List.of(
                MEMBER_CLAIMS_HEADER, ADMIN_HEADER, CLIENT_IP_HEADER);

        /** 待下发的身份头，值为 {@code null} 表示该头不下发。 */
        private final Map<String, String> identity;

        TrustedHeadersRequest(HttpServletRequest request, Map<String, String> identity) {
            super(request);
            this.identity = identity;
        }

        /** {@inheritDoc} */
        @Override
        public String getHeader(String name) {
            return isTrusted(name) ? value(name) : super.getHeader(name);
        }

        /** {@inheritDoc} */
        @Override
        public Enumeration<String> getHeaders(String name) {
            if (!isTrusted(name)) {
                return super.getHeaders(name);
            }
            String value = value(name);
            return value == null
                    ? Collections.emptyEnumeration()
                    : Collections.enumeration(List.of(value));
        }

        /** {@inheritDoc} */
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
            identity.forEach((name, value) -> {
                if (value != null) {
                    names.add(name);
                }
            });
            return Collections.enumeration(names);
        }

        private boolean isTrusted(String name) {
            return TRUSTED.stream().anyMatch(name::equalsIgnoreCase);
        }

        private String value(String name) {
            for (Map.Entry<String, String> entry : identity.entrySet()) {
                if (entry.getKey().equalsIgnoreCase(name)) {
                    return entry.getValue();
                }
            }
            return null;
        }
    }
}
