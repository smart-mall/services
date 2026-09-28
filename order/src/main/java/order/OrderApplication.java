package order;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 订单服务启动类。
 *
 * <p>{@code @EnableFeignClients} 未指定 {@code basePackages}，默认扫描本类所在包及其子包，
 * 因此 {@code order.feign} 下的远程接口无需再显式声明。
 *
 * <p>{@code @EnableScheduling} 使 {@code @Scheduled} 生效，本地消息表的兜底重投任务依赖它；
 * 缺了这条注解不会报错，但投递失败的消息永远不会被重投。
 */
@SpringBootApplication
@EnableFeignClients
@EnableScheduling
public class OrderApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderApplication.class, args);
    }

}
