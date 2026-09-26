package order;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 订单服务启动类。
 *
 * <p>{@code @EnableFeignClients} 未指定 {@code basePackages}，默认扫描本类所在包及其子包，
 * 因此 {@code order.feign} 下的远程接口无需再显式声明。
 */
@SpringBootApplication
@EnableFeignClients
public class OrderApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderApplication.class, args);
    }

}
