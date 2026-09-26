package auth.service.impl;

import auth.service.IpLocationService;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import common.utils.HttpClientUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * IP 归属地解析，走 ip-api.com 的免费接口。
 *
 * <p>该接口对回环 / 私网地址直接拒答（{@code reserved range} / {@code private range}），
 * 这类 IP 改用服务端出口 IP 查 —— 本地开发时恰好等于访问者的城市。任何失败都返回 null。</p>
 */
@Slf4j
@Service
public class IpLocationServiceImpl implements IpLocationService {

    /** 按指定 IP 查询的接口地址，{@code %s} 处填目标 IP。 */
    private static final String URL_BY_IP = "http://ip-api.com/json/%s?lang=zh-CN";
    /** 不指定 IP 的接口地址，返回的是服务端出口 IP 的归属地。 */
    private static final String URL_BY_EGRESS = "http://ip-api.com/json/?lang=zh-CN";

    /** 连接超时，单位毫秒。压得很短：这个调用挂在登录路径上，不能让它拖慢登录 */
    private static final int CONNECT_TIMEOUT_MS = 1000;
    /** 读取超时，单位毫秒。 */
    private static final int READ_TIMEOUT_MS = 2000;

    /** 缓存条目上限，超过就整体清空：IP 维度无法预估，不设上限会一直涨。 */
    private static final int CACHE_LIMIT = 512;

    /** 私网/回环 IP 在缓存里的键：它们查的都是同一个出口 IP。 */
    private static final String EGRESS_CACHE_KEY = "egress";

    /** 查不到时的缓存值。不缓存的话每次登录都要白跑一次外部请求 */
    private static final String NO_CITY = "";

    private final Map<String, String> cache = new ConcurrentHashMap<>();

    /** {@inheritDoc} */
    @Override
    public String resolveCity(String ip) {
        boolean publicIp = isPublic(ip);
        String key = publicIp ? ip : EGRESS_CACHE_KEY;

        String cached = cache.get(key);
        if (cached != null) {
            return cached.isEmpty() ? null : cached;
        }

        String city = query(publicIp ? String.format(URL_BY_IP, ip) : URL_BY_EGRESS);
        // 满了整体清空而不是淘汰：缓存只是省一次外部请求，丢了不影响正确性
        if (cache.size() >= CACHE_LIMIT) {
            cache.clear();
        }
        cache.put(key, city == null ? NO_CITY : city);
        return city;
    }

    /**
     * 请求接口并取出城市名。
     *
     * @param url 完整请求地址
     * @return 城市名；请求失败、状态不是 {@code success} 或城市为空时返回 {@code null}
     */
    private String query(String url) {
        try {
            JSONObject body = JSON.parseObject(
                    HttpClientUtils.get(url, "UTF-8", CONNECT_TIMEOUT_MS, READ_TIMEOUT_MS));
            if (body == null || !"success".equals(body.getString("status"))) {
                return null;
            }
            String city = body.getString("city");
            return city == null || city.isBlank() ? null : city;
        } catch (Exception e) {
            log.warn("IP 归属地解析失败: {}", url, e);
            return null;
        }
    }

    /**
     * 是否是公网地址。
     *
     * <p>判错的代价只是白跑一次请求（接口会拒答），所以只覆盖回环与三段私网。</p>
     */
    private boolean isPublic(String ip) {
        if (ip == null || ip.isBlank()) {
            return false;
        }
        if (ip.startsWith("127.") || "::1".equals(ip) || ip.startsWith("0:0:0:0:0:0:0:1")) {
            return false;
        }
        if (ip.startsWith("10.") || ip.startsWith("192.168.") || ip.startsWith("169.254.")) {
            return false;
        }
        if (ip.startsWith("172.")) {
            String[] parts = ip.split("\\.");
            if (parts.length > 1) {
                try {
                    int second = Integer.parseInt(parts[1]);
                    if (second >= 16 && second <= 31) {
                        return false;
                    }
                } catch (NumberFormatException ignored) {
                    // 不是合法 IP，交给接口去拒答
                }
            }
        }
        return true;
    }
}
