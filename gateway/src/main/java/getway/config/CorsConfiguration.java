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
 * <p>前端的 baseURL 用绝对地址是<b>刻意的设计</b>，不是待清理的临时代码，
 * 所以上面这份白名单是必需的、不能删：前端一旦换端口，或者别人 clone 下来跑在别的端口，
 * 都得同步往列表里加，否则浏览器会直接拦掉请求 —— 而报错是跨域，很容易误判成后端挂了。</p>
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
