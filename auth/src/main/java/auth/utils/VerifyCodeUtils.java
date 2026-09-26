package auth.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * 验证码工具：防刷判断、生成入库、校验、消耗，手机与邮箱两条链路共用。
 *
 * <p>value 格式是 {@code 验证码_写入时间戳}，key 的 TTL 同时是验证码有效期和防刷窗口；
 * 写入格式、key 构造与解析必须留在同一个类里，否则格式一变就会静默解析到错的内容。
 */
@Slf4j
public final class VerifyCodeUtils {

    /** 同一个目标两次发码的最小间隔，单位毫秒 */
    private static final long SEND_INTERVAL_MILLIS = 60_000L;

    /** value 里验证码与时间戳之间的分隔符 */
    private static final char SEPARATOR = '_';

    private VerifyCodeUtils() {
    }

    /**
     * 返回距离下次可以发码的剩余秒数。
     *
     * @param redis  验证码所在的 Redis 客户端
     * @param prefix key 前缀，区分短信与邮箱
     * @param target 接收方，手机号或邮箱
     * @return 剩余秒数，0 表示当前即可发送
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
     * 生成验证码连同写入时间戳存入 Redis，再交给 {@code sender} 真正发出。
     *
     * <p>写入先于发送：防刷靠的就是这个 key，必须先落库。发送失败时 {@code sender} 抛出的异常
     * 原样外传，此时 key 已存在，用户要等满 60 秒才能重发 —— 这是防刷应有的行为，不做回滚。</p>
     *
     * @param redis      验证码所在的 Redis 客户端
     * @param prefix     key 前缀，区分短信与邮箱
     * @param target     接收方，手机号或邮箱
     * @param ttlMinutes 验证码有效期，同时也是防刷窗口的上限
     * @param sender     把验证码送达用户的动作（短信走 third-party，邮箱走 Resend），失败时自行抛异常
     */
    public static void send(StringRedisTemplate redis, String prefix, String target,
                            int ttlMinutes, Consumer<String> sender) {
        // (Math.random() * 9 + 1) * 100000：保证首位非 0，且恒为 6 位
        String code = String.valueOf((int) ((Math.random() * 9 + 1) * 100000));

        redis.opsForValue().set(prefix + target, code + SEPARATOR + System.currentTimeMillis(),
                ttlMinutes, TimeUnit.MINUTES);

        sender.accept(code);
    }

    /**
     * 校验验证码是否正确。
     *
     * <p>只读不写，通过之后由调用方决定何时 {@link #consume}。</p>
     *
     * @param redis  验证码所在的 Redis 客户端
     * @param prefix key 前缀，区分短信与邮箱
     * @param target 接收方，手机号或邮箱
     * @param code   用户提交的验证码，可以为 {@code null}
     * @return {@code true} 表示匹配；无记录、已过期或 {@code code} 为 {@code null} 时返回 {@code false}
     */
    public static boolean verify(StringRedisTemplate redis, String prefix, String target, String code) {
        String stored = redis.opsForValue().get(prefix + target);
        if (StringUtils.isEmpty(stored) || code == null) {
            return false;
        }
        return code.equals(codeOf(stored));
    }

    /**
     * 消耗验证码（用过即废），只在业务成功之后调用。
     *
     * @param redis  验证码所在的 Redis 客户端
     * @param prefix key 前缀，区分短信与邮箱
     * @param target 接收方，手机号或邮箱
     */
    public static void consume(StringRedisTemplate redis, String prefix, String target) {
        redis.delete(prefix + target);
    }

    /**
     * 取 value 里的验证码部分。
     *
     * <p>没有分隔符时整个 value 就是验证码 —— 手工种进 Redis 的裸验证码照样能校验通过。
     *
     * @param stored Redis 里存的原始 value
     * @return 验证码部分
     */
    private static String codeOf(String stored) {
        int i = stored.indexOf(SEPARATOR);
        return i < 0 ? stored : stored.substring(0, i);
    }

    /**
     * 取 value 里的写入时间戳。
     *
     * <p>缺少分隔符或字段不是数字时都返回 0 而不抛异常，否则会冒到 GlobalExceptionHandler
     * 之外变成 500。
     *
     * @param stored Redis 里存的原始 value
     * @return 写入时间戳（毫秒）；读不出来时返回 0
     */
    private static long timestampOf(String stored) {
        int start = stored.indexOf(SEPARATOR);
        if (start < 0 || start == stored.length() - 1) {
            return 0L;
        }
        // 只取第一个分隔符之后的字段：value 万一多出第三个字段，同样忽略
        int end = stored.indexOf(SEPARATOR, start + 1);
        String field = end < 0 ? stored.substring(start + 1) : stored.substring(start + 1, end);
        try {
            return Long.parseLong(field);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }
}
