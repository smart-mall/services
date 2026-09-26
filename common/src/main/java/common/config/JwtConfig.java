package common.config;

import common.utils.JwtUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * JWT 装配：按 {@code jwt.secret} 与 {@code jwt.expire-hours} 创建 {@link JwtUtils}。
 *
 * <p>由 common 的 {@code AutoConfiguration.imports} 注册，会进入每个服务的容器；网关与 auth
 * 是两个进程，两边 secret 必须一致，否则 auth 签发的 token 网关验不过。
 */
@Configuration
public class JwtConfig {

    /**
     * 创建 JWT 工具，创建后即可签发与校验 token。
     *
     * @param secret      签名密钥，取自 {@code jwt.secret}，缺省为空串
     * @param expireHours token 有效期（小时），取自 {@code jwt.expire-hours}，缺省 168
     * @return 使用 HS256 签发与校验 token 的工具实例
     */
    @Bean
    public JwtUtils jwtUtils(@Value("${jwt.secret:}") String secret,
                             @Value("${jwt.expire-hours:168}") long expireHours) {
        return new JwtUtils(secret, expireHours);
    }
}
