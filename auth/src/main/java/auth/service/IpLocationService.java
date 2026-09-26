package auth.service;

/**
 * IP 归属地解析，供登录记录补充城市信息。
 *
 * <p>实现方不得抛异常：解析失败一律以 {@code null} 表示，登录链路不能被它阻断。
 */
public interface IpLocationService {

    /**
     * 解析 IP 对应的城市名。
     *
     * @param ip 客户端 IP，可以为 {@code null} 或空串
     * @return 城市名；解析不出来时返回 {@code null}
     */
    String resolveCity(String ip);
}
