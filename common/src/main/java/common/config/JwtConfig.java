package common.config;

import common.utils.JwtUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * JWT 的装配。
 *
 * <p>这个类会出现在<b>每一个</b>服务的容器里（common 的 AutoConfiguration.imports 里注册的，
 * 各服务都没有 scanBasePackages，靠这个文件生效）。实际用到它的只有三处：auth 签发登录的
 * token、gateway 验签、member 在修改资料后重签 token（昵称和头像在 token 里，不重签的话
 * 头部要等重新登录才会变）。其余服务只是白拿一个没人调用的 bean，不会自己去读 secret。</p>
 *
 * <p>因为网关和 auth 是两个进程，<b>两边的 secret 必须一模一样</b>，
 * 否则 auth 签出来的 token 网关一律验不过。配置在 common 的
 * {@code application-common.yaml} 顶层 {@code jwt} 下，每个服务都 import 了那个文件，
 * 所以网关和 auth 天然拿到同一个值。</p>
 */
@Configuration
public class JwtConfig {

    @Bean
    public JwtUtils jwtUtils(@Value("${jwt.secret:}") String secret,
                             @Value("${jwt.expire-hours:168}") long expireHours) {
        return new JwtUtils(secret, expireHours);
    }
}
