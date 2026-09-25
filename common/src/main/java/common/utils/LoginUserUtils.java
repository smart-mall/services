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

/** 下游服务从网关注入的请求头里取当前登录者，不解析凭证。 */
@Slf4j
public final class LoginUserUtils {

    /** Authorization 头里 token 的前缀。认证方案名大小写不敏感，但后面必须跟空白再接 token */
    private static final String BEARER_PREFIX = "Bearer ";

    private LoginUserUtils() {
    }

    /** 会员身份，编码进 {@code X-Member-Claims}。只编下面这几个字段，不要整个对象序列化 */
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

    /** 管理员身份，编码进 {@code X-Admin} */
    public static String encode(AdminResponseVo admin) {
        if (admin == null) {
            return null;
        }
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("id", admin.getId());
        claims.put("username", admin.getUsername());
        return encode(claims);
    }

    /** 当前登录会员；拿不到就抛 {@code 15004}（走 GlobalExceptionHandler 出 200 + code） */
    public static MemberResponseVo requireCurrentUser(HttpServletRequest request) {
        MemberResponseVo user = currentUser(request);
        if (user == null || user.getId() == null) {
            throw new BaseException(BaseCodeEnum.NOT_LOGIN_EXCEPTION);
        }
        return user;
    }

    /** 当前登录管理员；拿不到就抛 {@code 401}（管理端前端按 body 的 code 401 跳登录页） */
    public static AdminResponseVo requireCurrentAdmin(HttpServletRequest request) {
        AdminResponseVo admin = decode(header(request, ADMIN_HEADER), AdminResponseVo.class);
        if (admin == null || admin.getId() == null) {
            throw new BaseException(BaseCodeEnum.ADMIN_NOT_LOGIN_EXCEPTION);
        }
        return admin;
    }

    /** 从 Authorization 头取 Bearer token，格式不是 {@code Bearer <token>} 就返回 null */
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
