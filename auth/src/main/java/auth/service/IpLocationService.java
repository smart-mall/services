package auth.service;

/**
 * IP 归属地。
 */
public interface IpLocationService {

    /** 城市名；解析不出来返回 null */
    String resolveCity(String ip);
}
