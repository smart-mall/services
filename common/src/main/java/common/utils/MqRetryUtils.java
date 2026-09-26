package common.utils;

import org.springframework.amqp.core.Message;

import java.util.List;
import java.util.Map;

/**
 * RabbitMQ 重试相关工具。
 *
 * <p>"这条消息被死信过几次"这件事，broker 记在 {@code x-death} 消息头里，而且是
 * <b>持久化</b>的 —— 服务重启不会丢，比在消费端内存里数次数可靠。消费端靠它决定
 * "继续重试"还是"送进死信队列"。</p>
 */
public final class MqRetryUtils {

    private MqRetryUtils() {
    }

    /**
     * 这条消息在指定队列上被死信过几次，也就是已经重试过几次。
     *
     * <p>{@code x-death} 是个数组，元素形如 {@code {queue, reason, count, exchange, routing-keys, time}}。
     * 注意一次重试会在数组里留下<b>两条</b>记录：业务队列一条（reason=rejected），
     * 重试队列一条（reason=expired）。所以必须<b>按队列名过滤后累加</b>，
     * 不能简单取 {@code x-death[0].count}。</p>
     *
     * @param message 当前消费的消息，从它的消息头里读 {@code x-death}
     * @param queue 业务队列名
     * @return 已重试次数；首次投递（还没被死信过）时为 0
     */
    public static int attemptCount(Message message, String queue) {
        Object xDeath = message.getMessageProperties().getHeader("x-death");
        if (!(xDeath instanceof List<?> deaths)) {
            return 0;
        }

        int count = 0;
        for (Object death : deaths) {
            if (death instanceof Map<?, ?> entry
                    && queue.equals(entry.get("queue"))
                    && entry.get("count") instanceof Long times) {
                count += times;
            }
        }
        return count;
    }
}
