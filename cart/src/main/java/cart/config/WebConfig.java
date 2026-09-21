package cart.config;

import cart.interceptor.LoginUserInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;


/**
 * 购物车的 Web 配置。
 *
 * <p>拦截范围只写 {@code /cart/**}，不像 order / seckill 那样写 {@code /**}：
 * {@code management.endpoints.web.exposure.include: "*"} 把所有 actuator 端点都暴露出来了，
 * 写 {@code /**} 会把 {@code /actuator/**} 一起拦成 401，健康检查和指标全查不了。
 * 购物车自己的接口全在 {@code /cart} 下面，收紧到这一段没有任何损失。</p>
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final LoginUserInterceptor loginUserInterceptor;

    public WebConfig(LoginUserInterceptor loginUserInterceptor) {
        this.loginUserInterceptor = loginUserInterceptor;
    }

    /**
     * 购物车的所有接口都要求登录，没有匿名购物车（原因见 {@link common.constant.CartConstant}）。
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loginUserInterceptor).addPathPatterns("/cart/**");
    }
}
