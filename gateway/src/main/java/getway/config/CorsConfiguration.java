package getway.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 跨域配置：放行前端开发服务器与管理后台前端的跨源请求。
 *
 * <p>下面开启了 {@code allowCredentials}，带凭据的请求不允许通配源，因此只能逐个列出具体源；
 * 前端换端口或社交登录回跳地址（auth 的 {@code auth.front-url}）变更后未同步，浏览器会直接拦掉请求。
 */
@Configuration
public class CorsConfiguration implements WebMvcConfigurer {
    /** {@inheritDoc} */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(
                        // 管理后台前端（renren-fast 配套）
                        "http://localhost:56731", "http://localhost:56732",
                        // 前台 SPA（vite dev server 的默认端口）
                        "http://localhost:5173", "http://127.0.0.1:5173",
                        // 配了 hosts 后用域名访问的情况
                        "http://gulimall.com:5173")
                .allowCredentials(true)
                .allowedMethods("GET", "POST", "DELETE", "PUT")
                .maxAge(3600);
    }

}
