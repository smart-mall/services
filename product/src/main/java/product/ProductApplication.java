package product;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * product 商品服务的启动类。
 *
 * <p>{@code @EnableFeignClients} 打开 Feign 客户端扫描：商品详情、上架与删除要远程调用
 * seckill、ware、search、coupon、third-party 五个下游服务。
 */
@SpringBootApplication
@EnableFeignClients
// 本地消息表的重投与清理任务需要它（OutboxResendTask）
@EnableScheduling
public class ProductApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProductApplication.class, args);
    }

}
