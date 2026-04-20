package common.config;


import feign.RequestInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

@Configuration
@Slf4j
public class FeignConfig {

    @Bean("requestInterceptor")
    public RequestInterceptor requestInterceptor() {
        //1、使用RequestContextHolder拿到刚进来的请求数据
        //老请求
        //2、同步请求头的数据（主要是cookie）
        //把老请求的cookie值放到新请求上来，进行一个同步
        return template -> {
            //1、使用RequestContextHolder拿到刚进来的请求数据
            ServletRequestAttributes requestAttributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

            if (requestAttributes != null) {
                //老请求
                HttpServletRequest request = requestAttributes.getRequest();

                //2、同步请求头的数据（主要是cookie）
                //把老请求的cookie值放到新请求上来，进行一个同步
                String cookie = request.getHeader("Cookie");
                log.debug("解决feign远程调用丢失请求头问题：在新请求添加Cookie:{}", cookie);
                template.header("Cookie", cookie);
            }
        };
    }

}
