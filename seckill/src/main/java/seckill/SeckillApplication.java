package seckill;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 秒杀服务启动类。
 *
 * <p>本服务不直连数据库，因此排除 {@code DataSourceAutoConfiguration}：
 * 保留该自动配置时，没有数据源参数会导致启动直接失败。
 */
@EnableFeignClients
@SpringBootApplication(exclude = DataSourceAutoConfiguration.class)
public class SeckillApplication {

    public static void main(String[] args) {
        SpringApplication.run(SeckillApplication.class, args);
    }

}
