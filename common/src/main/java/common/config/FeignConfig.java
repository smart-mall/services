package common.config;


import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

import static common.constant.AuthServerConstant.MEMBER_CLAIMS_HEADER;

/**
 * Feign 请求拦截器：把当前登录身份透传给下游服务。
 *
 * <p>Feign 调用是全新的 HTTP 请求，不继承上游请求头，身份必须在此手动搬运。
 *
 * <p>必须透传 {@link common.constant.AuthServerConstant#MEMBER_CLAIMS_HEADER}：缺少它下游
 * 识别不出用户，表现为返回空数据或 401，且报错点通常远离此处，排查成本高。
 *
 * <p>Cookie 与 token 透传已无消费者，仅为兼容旧登录态保留，删除前需确认下游均不读取。
 */
@Configuration
@Slf4j
public class FeignConfig {

    /**
     * 返回 Feign 请求拦截器，在每次远程调用前补齐身份相关的请求头。
     *
     * @return 透传当前请求上下文的拦截器；没有请求上下文时不附加任何头
     */
    @Bean("requestInterceptor")
    public RequestInterceptor requestInterceptor() {
        return template -> {
            ServletRequestAttributes requestAttributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

            // 没有绑定请求上下文就什么都不带：Feign 也可能在 MQ 消费、定时任务、
            // 或自己开的线程池里被调用，那些场景本来就没有"当前请求"
            if (requestAttributes == null) {
                return;
            }
            HttpServletRequest request = requestAttributes.getRequest();

            // TODO: 确认所有下游服务都不再读取后，删除 Cookie 与 token 的透传
            copyHeader(template, request, "Cookie");
            copyHeader(template, request, "token");
            copyHeader(template, request, MEMBER_CLAIMS_HEADER);
        };
    }

    /**
     * 上游请求带了这个头才转发。
     *
     * <p>值为空时不能直接丢给 Feign：那会发出去一个空头，下游解出来一样是"未登录"，
     * 但日志里看起来像是"带了身份只是没解开"，反而误导排查。干脆不发。
     *
     * @param template 待发出的 Feign 请求模板
     * @param request  当前上游请求
     * @param name     要透传的请求头名
     */
    private void copyHeader(RequestTemplate template, HttpServletRequest request, String name) {
        String value = request.getHeader(name);
        if (value != null && !value.isBlank()) {
            template.header(name, value);
            log.debug("Feign 转发请求头 {}：{}", name, value);
        }
    }

}
