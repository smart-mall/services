package getway.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 跨域配置。
 *
 * <p>前端的 request.ts 里 baseURL 写的是绝对地址 {@code http://localhost:53000/api}，
 * 也就是从 {@code localhost:5173} 打到 {@code localhost:53000} —— 端口不同就是跨域，
 * 必须在这里放行，否则浏览器直接把请求拦掉（控制台报 No 'Access-Control-Allow-Origin'）。
 * 而且它开了 {@code withCredentials: true}，所以 allowedOrigins 必须是<b>具体的源</b>、
 * 不能图省事写 {@code *}（带凭据的请求不允许通配源），下面这几个就是这么来的。</p>
 *
 * <p>注册社交登录回跳的前端地址（auth 的 {@code auth.front-url}）要在这个列表里，
 * 否则回跳页拿到了 token 也调不通接口。</p>
 *
 * <p>另一种做法是前端把 baseURL 改成相对路径 {@code /api} + vite 的 server.proxy 走同源，
 * 那样一条 CORS 配置都不需要。现在两种都留着。</p>
 */
@Configuration
public class CorsConfiguration implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(
                        // 后台管理（renren-fast 那个前端用的）
                        "http://localhost:56731", "http://localhost:56732",
                        // 前台 SPA（vite dev server 的默认端口）
                        "http://localhost:5173", "http://127.0.0.1:5173",
                        // 加了 hosts 之后用域名访问的情况
                        "http://gulimall.com:5173")
                .allowCredentials(true)
                .allowedMethods("GET", "POST", "DELETE", "PUT")
                .maxAge(3600);
    }

}
