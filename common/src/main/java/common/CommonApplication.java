package common;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * common 模块的 Spring Boot 启动入口。
 *
 * <p>common 以普通 jar 被各服务依赖，配置类由 {@code AutoConfiguration.imports} 注册，
 * 业务服务不需要启动本类；它只用于在 common 内单独拉起一个容器。
 */
@SpringBootApplication
public class CommonApplication {

	public static void main(String[] args) {
		SpringApplication.run(CommonApplication.class, args);
	}

}
