package common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 线程池参数，绑定配置前缀 {@code thread}（{@code coreSize} / {@code maxSize} / {@code keepAliveTime}）。
 *
 * <p>由 {@link MyThreadConfig} 类上的 {@code @EnableConfigurationProperties} 注册为 bean，
 * 自身没有 {@code @Component}，组件扫描不到它。
 */
@ConfigurationProperties(prefix = "thread")
@Data
public class ThreadPoolConfigProperties {

    /** 核心线程数。 */
    private Integer coreSize;

    /** 最大线程数。 */
    private Integer maxSize;

    /** 非核心线程空闲存活时间，单位秒（由 {@link MyThreadConfig} 按 {@code TimeUnit.SECONDS} 使用）。 */
    private Integer keepAliveTime;


}
