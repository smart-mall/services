package coupon;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * coupon 服务的启动类，同时开启 Feign 客户端扫描。
 *
 * <p>对外由网关按 {@code /api/coupon/**} 转发进来，服务间调用走 {@code @FeignClient("coupon")} 直连。
 */
@SpringBootApplication
@EnableFeignClients
public class CouponApplication {

    public static void main(String[] args) {
        SpringApplication.run(CouponApplication.class, args);
    }

}
