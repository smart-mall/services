package common.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


/**
 * 业务线程池装配：按 {@code thread} 前缀的配置创建全局 {@link ThreadPoolExecutor}。
 *
 * <p>类上的 {@code @EnableConfigurationProperties} 负责注册
 * {@link ThreadPoolConfigProperties}；队列容量与拒绝策略在此硬编码。
 */
@EnableConfigurationProperties(ThreadPoolConfigProperties.class)
@Configuration
public class MyThreadConfig {


    /**
     * 创建全局业务线程池。
     *
     * <p>拒绝策略是 {@link ThreadPoolExecutor.AbortPolicy}：队列满时直接抛异常，不静默丢任务。
     *
     * @param pool {@code thread} 前缀绑定的线程池参数
     * @return 核心/最大线程数与空闲存活时间均取自配置的线程池
     */
    @Bean
    public ThreadPoolExecutor threadPoolExecutor(ThreadPoolConfigProperties pool) {
        return new ThreadPoolExecutor(
                pool.getCoreSize(),
                pool.getMaxSize(),
                pool.getKeepAliveTime(),
                TimeUnit.SECONDS,
                // 队列容量给到 10 万，只有堆积到这一步才会触发 AbortPolicy 抛拒绝异常
                new LinkedBlockingDeque<>(100000),
                Executors.defaultThreadFactory(),
                new ThreadPoolExecutor.AbortPolicy()
        );
    }

}
