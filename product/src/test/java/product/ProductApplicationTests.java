package product;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

/** Redis 连通性检查：能往 Redis 写进一个键，就说明连接与序列化都正常。 */
@SpringBootTest
class ProductApplicationTests {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /** 写入一个测试键。 */
    @Test
    void contextLoads() {
        stringRedisTemplate.opsForValue().set("name", "张三");
    }

}
