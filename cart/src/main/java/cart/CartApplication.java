package cart;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 购物车服务的启动类。
 *
 * <p>开启 Feign 客户端扫描，用于调用商品服务查询 SKU 信息与最新价格。
 */
@EnableFeignClients
@SpringBootApplication
public class CartApplication {

    public static void main(String[] args) {
        SpringApplication.run(CartApplication.class, args);
    }

}
