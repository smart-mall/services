package auth.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * 验证码的完整生命周期：防刷判断、生成入库、校验、消耗。
 *
 * <p>手机（{@link auth.controller.SmsAuthController}）和邮箱（{@link auth.controller.EmailAuthController}）
 * 两条链路唯一的差别就是 Redis key 前缀，所以收在这里，只留一份实现。</p>
 *
 * <p><b>为什么防刷和校验必须在同一个类里：</b>防刷之所以能成立，靠的是它复用了验证码 value 里
 * 那个写入时间戳 —— 「同一个 key 还在 → 说明刚发过」。也就是说防刷和校验是
 * {@code 验证码_时间戳} 这同一个数据结构的两个读者。只把「读时间戳、比 60 秒」抽出来做成
 * 限流工具，写入格式和 key 的构造仍然留在两个调用方，工具的隐含前提没人管得住，
 * 下次改格式它只会静默失效（{@code split} 不会报错，只会解析到错的东西）。</p>
 *
 * <p>value 格式是 {@code 验证码_写入时间戳}；key 的 TTL 同时承担两件事：验证码有效期，
 * 以及上面说的防刷信号。</p>
 *
 * <p><b>为什么是静态工具而不是 bean：</b>各服务的 {@code @SpringBootApplication} 都在自己包下，
 * 没有配 {@code scanBasePackages}，common 的 bean 只靠
 * {@code META-INF/spring/...AutoConfiguration.imports} 注册。这个类没有任何状态，
 * 所以做成静态方法、{@link StringRedisTemplate} 由调用方传进来，省掉一次注册。
 * 同理它没有放进 common —— 目前唯一的使用者是 auth，放过去等于给另外 10 个用不到的服务都带上它。</p>
 *
 * <p><b>verify 和 consume 是分开的，不要合并成一个 verifyAndConsume：</b>
 * 「业务失败不删码、成功才删」是有意的 —— 注册时用户名/邮箱撞了属于业务失败，
 * 用户改个名字就该能用同一个验证码重试；而且不删还意味着失败的注册不会把 60 秒防刷窗口重置掉。</p>
 */
@Slf4j
public final class VerifyCodeUtils {

    /** 同一个目标两次发码的最小间隔 */
    private static final long SEND_INTERVAL_MILLIS = 60_000L;

    /** value 里验证码和时间戳之间的分隔符 */
    private static final char SEPARATOR = '_';

    private VerifyCodeUtils() {
    }

    /**
     * 距离下次可以发码还有多少秒，返回 0 表示现在就能发。
     *
     * <p>返回剩余秒数而不是 boolean，是为了将来前端要显示「请等待 N 秒后重试」时不用再动工具类；
     * 目前两个调用方都只判断 {@code > 0}，返回值不参与响应。</p>
     */
    public static long remainingSeconds(StringRedisTemplate redis, String prefix, String target) {
        String stored = redis.opsForValue().get(prefix + target);
        if (StringUtils.isEmpty(stored)) {
            return 0L;
        }

        long writtenAt = timestampOf(stored);
        if (writtenAt <= 0L) {
            // value 不是本工具写的（例如测试时手工往 Redis 种了一个裸验证码），
            // 读不出时间戳就判断不了窗口。按「可以发送」处理：既不 500，也不会把人永久卡住。
            log.warn("Redis 里的验证码没有时间戳，跳过防刷判断: key={}, value={}", prefix + target, stored);
            return 0L;
        }

        long elapsed = System.currentTimeMillis() - writtenAt;
        if (elapsed >= SEND_INTERVAL_MILLIS) {
            return 0L;
        }
        // 向上取整：还剩 1.2 秒也报 2 秒
        return (SEND_INTERVAL_MILLIS - elapsed + 999L) / 1000L;
    }

    /**
     * 生成验证码、连同当前时间戳写进 Redis，再交给 {@code sender} 真正发出去。
     *
     * <p>写入在发送<b>之前</b>：防刷靠的就是这个 key，必须先落库。发送失败时 {@code sender}
     * 抛出的异常会原样往外传，此时 key 已存在，用户要等满 60 秒才能重发 —— 这是防刷该有的
     * 行为，不是 bug，所以这里不做「失败就回滚 key」。</p>
     *
     * @param ttlMinutes 验证码有效期，同时也是防刷窗口的上限
     * @param sender     把验证码送达用户这个动作（短信走 third-party，邮箱走 Resend）；
     *                   失败时自己抛异常，这里原样往外传
     */
    public static void send(StringRedisTemplate redis, String prefix, String target,
                            int ttlMinutes, Consumer<String> sender) {
        // 保持原来的写法：(Math.random() * 9 + 1) * 100000 保证首位非 0、恒为 6 位
        String code = String.valueOf((int) ((Math.random() * 9 + 1) * 100000));

        redis.opsForValue().set(prefix + target, code + SEPARATOR + System.currentTimeMillis(),
                ttlMinutes, TimeUnit.MINUTES);

        sender.accept(code);
    }

    /** 校验验证码是否正确，只读不写；通过之后由调用方决定何时 {@link #consume}。 */
    public static boolean verify(StringRedisTemplate redis, String prefix, String target, String code) {
        String stored = redis.opsForValue().get(prefix + target);
        if (StringUtils.isEmpty(stored) || code == null) {
            return false;
        }
        return code.equals(codeOf(stored));
    }

    /** 消耗验证码（用过即废）。只在业务成功之后调用。 */
    public static void consume(StringRedisTemplate redis, String prefix, String target) {
        redis.delete(prefix + target);
    }

    /**
     * 取 value 里的验证码部分。
     *
     * <p>没有分隔符时整个 value 就是验证码 —— 原来用的 {@code split("_")[0]} 本来就是这个语义，
     * 所以保持一致，手工种进去的裸验证码照样能校验通过。</p>
     */
    private static String codeOf(String stored) {
        int i = stored.indexOf(SEPARATOR);
        return i < 0 ? stored : stored.substring(0, i);
    }

    /**
     * 取 value 里的时间戳，读不出来返回 0。
     *
     * <p>原来两处都是直接 {@code Long.parseLong(stored.split("_")[1])}：value 里没有下划线时
     * 数组越界、内容不是数字时数字格式异常，两种都会冒到 GlobalExceptionHandler 之外变成 500。
     * 这里统一按「读不出来」处理，不再抛异常。</p>
     */
    private static long timestampOf(String stored) {
        int start = stored.indexOf(SEPARATOR);
        if (start < 0 || start == stored.length() - 1) {
            return 0L;
        }
        // 只取第二个字段，和原来的 split("_")[1] 严格等价（万一以后有第三个字段，同样忽略）
        int end = stored.indexOf(SEPARATOR, start + 1);
        String field = end < 0 ? stored.substring(start + 1) : stored.substring(start + 1, end);
        try {
            return Long.parseLong(field);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }
}
