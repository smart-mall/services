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
 * Feign 远程调用时把"当前是谁"带过去。
 *
 * <p>Feign 调用是一条全新的 HTTP 请求，不会继承原来那条请求的任何头，所以要有人手动搬。
 * 这里搬三样东西，但它们的性质完全不同：</p>
 *
 * <ul>
 *   <li><b>Cookie / token</b> —— Session 时代留下的。那时登录态在 HttpSession 里，
 *       下游要读就得把 session cookie（和网关透传的 token）一起带过去。
 *       现在登录态是 JWT + 网关验签后注入的请求头，这两个已经没有任何消费者，
 *       留着只是因为删之前要先确认所有服务都不读，属于待清理项。</li>
 *   <li><b>{@link common.constant.AuthServerConstant#MEMBER_CLAIMS_HEADER}</b> —— 现在真正
 *       决定"你是谁"的头。少了它的症状很难查：order 调 cart 取购物车时 cart 眼里
 *       order 是个匿名访客，于是返回空车或者直接 401，而报错点（order 里的 NPE）
 *       离根因（这一行）隔了好几个服务。</li>
 * </ul>
 */
@Configuration
@Slf4j
public class FeignConfig {

    @Bean("requestInterceptor")
    public RequestInterceptor requestInterceptor() {
        return template -> {
            // 1、使用RequestContextHolder拿到刚进来的请求数据
            ServletRequestAttributes requestAttributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

            // 没有绑定请求上下文就什么都不带：Feign 也可能在 MQ 消费、定时任务、
            // 或自己开的线程池里被调用，那些场景本来就没有"当前请求"
            if (requestAttributes == null) {
                return;
            }
            HttpServletRequest request = requestAttributes.getRequest();

            copyHeader(template, request, "Cookie");
            copyHeader(template, request, "token");
            copyHeader(template, request, MEMBER_CLAIMS_HEADER);
        };
    }

    /**
     * 原请求有这个头才转发。
     *
     * <p>值为 null 时不能直接丢给 Feign：那会发出去一个空头，下游解出来一样是"未登录"，
     * 但日志里看起来像是"带了身份只是没解开"，反而误导排查。干脆不发。</p>
     */
    private void copyHeader(RequestTemplate template, HttpServletRequest request, String name) {
        String value = request.getHeader(name);
        if (value != null && !value.isBlank()) {
            template.header(name, value);
            log.debug("Feign 转发请求头 {}：{}", name, value);
        }
    }

}
