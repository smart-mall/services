package ware;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * ware 服务启动类。
 *
 * <p>对外提供库存锁定与解锁、仓库与库存查询、采购单流转接口，通过 RabbitMQ 消费库存释放与商品删除事件，
 * 并由 {@code @EnableFeignClients} 扫描 {@code ware.feign} 下的下游服务客户端。
 */
@SpringBootApplication
@EnableFeignClients
public class WareApplication {

    public static void main(String[] args) {
        SpringApplication.run(WareApplication.class, args);
    }

}
