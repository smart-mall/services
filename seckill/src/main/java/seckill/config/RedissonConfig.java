package seckill.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;



/**
 * Redisson 客户端配置：秒杀的库存信号量与上架分布式锁都依赖这个客户端。
 *
 * <p>只配置单机模式，连接参数取自配置项 {@code redis.url}、{@code redis.port}、{@code redis.password}。
 */
@Configuration
public class  RedissonConfig {

    /** Redis 主机地址，来自配置项 {@code redis.url}。 */
    @Value("${redis.url}")
    private String host;

    /** Redis 端口，来自配置项 {@code redis.port}。 */
    @Value("${redis.port}")
    private int port;

    /** Redis 密码，来自配置项 {@code redis.password}。 */
    @Value("${redis.password}")
    private String password;


    /**
     * 创建单机模式的 Redisson 客户端。
     *
     * <p>Bean 销毁时自动调用 {@code shutdown()} 关闭底层连接，容器停止时不会残留连接。
     *
     * @return Redisson 客户端，可直接用于获取信号量与分布式锁
     */
    @Bean(destroyMethod="shutdown")
    public RedissonClient redissonClient() {
        Config config = new Config();
        config.useSingleServer()
                .setAddress("redis://" + host + ":" + port)
                .setPassword(password);

        return Redisson.create(config);
    }

}
