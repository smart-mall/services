package auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;


/**
 * 认证服务启动类：承载账号密码、邮箱验证码、手机验证码与社交（微博 / QQ）四条登录链路。
 *
 * <p>启用 Feign 客户端，用于调用 member 与 third-party 两个服务。
 */
@SpringBootApplication
@EnableFeignClients
public class AuthApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthApplication.class, args);
    }

}
