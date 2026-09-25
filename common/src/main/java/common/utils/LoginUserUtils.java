package common.utils;

import com.alibaba.fastjson.JSON;
import common.exception.BaseCodeEnum;
import common.vo.MemberResponseVo;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

import static common.constant.AuthServerConstant.MEMBER_CLAIMS_HEADER;

/**
 * 下游服务从请求头里取当前登录用户。
 *
 * <p>登录态改成 JWT 之后的链路：前端把 token 放在 {@code Authorization: Bearer xxx}，
 * gateway 统一验签，然后把用户信息塞进 {@link common.constant.AuthServerConstant#MEMBER_CLAIMS_HEADER}
 * 请求头往下一层传，业务服务只认这个头 —— 不再解析 JWT、不再读 HttpSession，
 * 所以下游服务不需要引 jjwt。</p>
 *
 * <p>为什么头里的值要 Base64URL 再放：昵称可能是中文，而 HTTP 头按 ISO-8859-1 处理，
 * 直接塞中文取出来就是乱码。Base64 之后是纯 ASCII，怎么传都不会坏。</p>
 */
@Slf4j
public final class LoginUserUtils {

    private LoginUserUtils() {
    }

    /** 把用户信息编码成可以放进请求头的 ASCII 串。只编下面这几个字段，不要整个对象序列化 */
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
        // fastjson 默认不输出 null 字段
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(JSON.toJSONBytes(claims));
    }

    /** 解不出来就返回 null（当作未登录），不要把异常抛给业务 */
    public static MemberResponseVo decode(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            byte[] json = Base64.getUrlDecoder().decode(value);
            return JSON.parseObject(new String(json, StandardCharsets.UTF_8), MemberResponseVo.class);
        } catch (Exception e) {
            log.warn("解析 {} 失败，按未登录处理", MEMBER_CLAIMS_HEADER, e);
            return null;
        }
    }

    /** 当前登录用户；未登录返回 null */
    public static MemberResponseVo currentUser(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        return decode(request.getHeader(MEMBER_CLAIMS_HEADER));
    }

    /**
     * 未登录时的统一响应：<b>HTTP 401</b> + {@code {code,msg}}。
     *
     * <p>为什么状态码必须是真 401：前端 request.ts 判断的是 HTTP 状态码 401 去清 localStorage 里的
     * token，把 401 塞进 body 的 code 字段它认不出来。</p>
     *
     * <p>charset 也要显式写：Spring Boot 3.5 起不再给 application/json 自动补 charset，
     * 少了它中文在部分客户端（比如 PowerShell 的 Invoke-RestMethod）会被按 Latin-1 解成乱码。</p>
     */
    public static void writeUnauthorized(HttpServletResponse response) throws IOException {
        writeUnauthorized(response, BaseCodeEnum.NOT_LOGIN_EXCEPTION);
    }

    /**
     * 同上，但错误码由调用方给。
     *
     * <p>需要它的只有一种情况：token <b>过期</b>要报 15005（前端提示"登录已过期"）而不是
     * 15004（"请先登录"）。拦截器那边只遇到"这个头压根没有"，所以用上面那个无参版本。</p>
     */
    public static void writeUnauthorized(HttpServletResponse response, BaseCodeEnum codeEnum) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(JSON.toJSONString(R.error(codeEnum.getCode(), codeEnum.getMsg())));
    }
}
