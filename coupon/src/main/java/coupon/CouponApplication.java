package coupon;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * coupon 服务的启动类，同时开启 Feign 客户端扫描与定时任务。
 *
 * <p>对外由网关按 {@code /api/coupon/**} 转发进来，服务间调用走 {@code @FeignClient("coupon")} 直连。
 *
 * <p>{@code @EnableScheduling} 使 {@code @Scheduled} 生效，已领到手的券的过期清理依赖它。
 */
@SpringBootApplication
@EnableFeignClients
@EnableScheduling
public class CouponApplication {

    public static void main(String[] args) {
        SpringApplication.run(CouponApplication.class, args);
    }

}
