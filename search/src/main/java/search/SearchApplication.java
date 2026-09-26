package search;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * search 服务的 Spring Boot 启动入口，负责拉起本模块的组件扫描与自动配置。
 *
 * <p>类上开启了 Feign 客户端扫描，本模块的 Feign 接口依赖它注册成 Bean。
 */
@SpringBootApplication
@EnableFeignClients
public class SearchApplication {

	public static void main(String[] args) {
		SpringApplication.run(SearchApplication.class, args);
	}

}
