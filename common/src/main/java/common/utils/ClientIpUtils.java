package common.utils;

import jakarta.servlet.http.HttpServletRequest;

import static common.constant.AuthServerConstant.CLIENT_IP_HEADER;

/**
 * 下游服务从请求头里取客户端 IP。
 *
 * <p>值由网关注入（见 {@link common.constant.AuthServerConstant#CLIENT_IP_HEADER}）：
 * 请求经网关转发之后，下游的 remoteAddr 是网关自己，取不到真实客户端地址。</p>
 */
public final class ClientIpUtils {

    private ClientIpUtils() {
    }

    /**
     * 返回当前请求的客户端 IP。
     *
     * @param request 当前请求，可为 {@code null}
     * @return 客户端 IP；网关没注入（或直连服务端口）时返回 {@code null}
     */
    public static String currentIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String ip = request.getHeader(CLIENT_IP_HEADER);
        return ip == null || ip.isBlank() ? null : ip.trim();
    }
}
