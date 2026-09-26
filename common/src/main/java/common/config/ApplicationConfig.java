package common.config;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.Configuration;

/**
 * 自动配置类，同时声明服务名 {@code common} 的 Feign 客户端。
 *
 * <p>类体为空，不定义任何远程接口；由 common 的 {@code AutoConfiguration.imports} 注册。
 */
@Configuration
@FeignClient(name = "common")
public class ApplicationConfig {
}
