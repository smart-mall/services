package getway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 网关服务启动类：启动 Spring Boot 容器，并开启 Feign 客户端扫描。
 *
 * <p>开启扫描后 {@link getway.feign.AdminAuthFeignService} 这类远程调用接口才能被注入使用。
 */
@SpringBootApplication
@EnableFeignClients
public class GatewayApplication {

	public static void main(String[] args) {
		SpringApplication.run(GatewayApplication.class, args);
	}

}
