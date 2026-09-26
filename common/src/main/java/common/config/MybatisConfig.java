package common.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 分页插件装配：注册全局唯一的 {@link MybatisPlusInterceptor} 并挂上分页内部拦截器。
 *
 * <p>不带配置前缀，由 common 的 {@code AutoConfiguration.imports} 注册为自动配置类。
 * 方言固定为 {@link DbType#MYSQL}；没有这个拦截器时 {@code Page} 查询不会追加 LIMIT。
 */
@Configuration
public class MybatisConfig {
    /**
     * 提供 MyBatis-Plus 拦截器，分页能力由它挂载的内部拦截器提供。
     *
     * @return 已挂载 MySQL 分页内部拦截器的拦截器实例
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }
}
