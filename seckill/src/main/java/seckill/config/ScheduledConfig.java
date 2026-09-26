package seckill.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;


/**
 * 定时任务与异步能力的开关配置。
 *
 * <p>{@code @EnableScheduling} 使 {@code @Scheduled} 生效，秒杀上架任务依赖它；
 * Spring Boot 默认不开启定时任务，缺了本类上架任务不会执行。
 *
 * <p>{@code @EnableAsync} 开启异步方法支持，本模块目前没有 {@code @Async} 方法。
 */
@EnableAsync
@EnableScheduling
@Configuration
public class ScheduledConfig {

}
