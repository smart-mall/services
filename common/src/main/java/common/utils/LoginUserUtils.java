package common.utils;

import com.alibaba.fastjson.JSON;
import common.exception.BaseCodeEnum;
import common.exception.BaseException;
import common.vo.AdminResponseVo;
import common.vo.MemberResponseVo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

import static common.constant.AuthServerConstant.ADMIN_HEADER;
import static common.constant.AuthServerConstant.MEMBER_CLAIMS_HEADER;

/**
 * 登录身份工具：下游服务从网关注入的请求头里取当前登录者，不解析凭证。
 *
 * <p>身份头由 gateway 写入，值为 Base64URL 编码的 JSON，头名见
 * {@link common.constant.AuthServerConstant#MEMBER_CLAIMS_HEADER} 与
 * {@link common.constant.AuthServerConstant#ADMIN_HEADER}。头缺失或解不出来时一律按未登录处理。</p>
 */
@Slf4j
public final class LoginUserUtils {

    /** Authorization 头里 token 的前缀。认证方案名大小写不敏感，但后面必须跟空白再接 token */
    private static final String BEARER_PREFIX = "Bearer ";

    private LoginUserUtils() {
    }

    /**
     * 把会员身份编码成 {@code X-Member-Claims} 头的值。
     *
     * <p>只编下面这几个字段，不要整个对象序列化：password 与 accessToken 不能出现在请求头里。
     *
     * @param user 登录会员信息
     * @return Base64URL 编码的 JSON；{@code user} 为 {@code null} 时返回 {@code null}
     */
    public static String encode(MemberResponseVo user) {
        if (user == null) {
            return null;
        }
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("id", user.getId());
        claims.put("username", user.getUsername());
        claims.put("nickname", user.getNickname());
        claims.put("header", user.getHeader());
        claims.put("integration", user.getIntegration());
        return encode(claims);
    }

    /**
     * 把管理员身份编码成 {@code X-Admin} 头的值。
     *
     * @param admin 登录管理员信息
     * @return Base64URL 编码的 JSON；{@code admin} 为 {@code null} 时返回 {@code null}
     */
    public static String encode(AdminResponseVo admin) {
        if (admin == null) {
            return null;
        }
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("id", admin.getId());
        claims.put("username", admin.getUsername());
        return encode(claims);
    }

    /**
     * 返回当前登录会员，取不到直接抛异常。
     *
     * @param request 当前请求，身份取自 {@code X-Member-Claims} 头
     * @return 登录会员，id 一定不为 {@code null}
     * @throws BaseException 头缺失或解析不出身份时抛出，错误码 15004，由 GlobalExceptionHandler 转成 200 + code
     */
    public static MemberResponseVo requireCurrentUser(HttpServletRequest request) {
        MemberResponseVo user = currentUser(request);
        if (user == null || user.getId() == null) {
            throw new BaseException(BaseCodeEnum.NOT_LOGIN_EXCEPTION);
        }
        return user;
    }

    /**
     * 返回当前登录管理员，取不到直接抛异常。
     *
     * @param request 当前请求，身份取自 {@code X-Admin} 头
     * @return 登录管理员，id 一定不为 {@code null}
     * @throws BaseException 头缺失或解析不出身份时抛出，错误码 401，管理端前端按响应体里的 code 跳登录页
     */
    public static AdminResponseVo requireCurrentAdmin(HttpServletRequest request) {
        AdminResponseVo admin = decode(header(request, ADMIN_HEADER), AdminResponseVo.class);
        if (admin == null || admin.getId() == null) {
            throw new BaseException(BaseCodeEnum.ADMIN_NOT_LOGIN_EXCEPTION);
        }
        return admin;
    }

    /**
     * 从 Authorization 头里取出 Bearer token。
     *
     * @param header Authorization 头的值，可为 {@code null}
     * @return token 字符串；格式不是 {@code Bearer <token>} 或 token 为空时返回 {@code null}
     */
    public static String resolveBearer(String header) {
        if (header == null) {
            return null;
        }
        String value = header.trim();
        if (!value.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            return null;
        }
        String token = value.substring(BEARER_PREFIX.length()).trim();
        return token.isEmpty() ? null : token;
    }

    private static MemberResponseVo currentUser(HttpServletRequest request) {
        return decode(header(request, MEMBER_CLAIMS_HEADER), MemberResponseVo.class);
    }

    private static String header(HttpServletRequest request, String name) {
        return request == null ? null : request.getHeader(name);
    }

    /** 解不出来返回 null，当作未登录 */
    private static <T> T decode(String value, Class<T> type) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            byte[] json = Base64.getUrlDecoder().decode(value);
            return JSON.parseObject(new String(json, StandardCharsets.UTF_8), type);
        } catch (Exception e) {
            log.warn("解析网关注入的身份失败，按未登录处理", e);
            return null;
        }
    }

    /** Base64URL：HTTP 头按 ISO-8859-1 处理，中文昵称直接塞进去会乱码；fastjson 默认不输出 null 字段 */
    private static String encode(Map<String, Object> claims) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(JSON.toJSONBytes(claims));
    }
}
