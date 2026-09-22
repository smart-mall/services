package product.mq;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import product.service.MqMessageService;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

/**
 * 本地消息表的兜底任务：重投 + 清理。
 *
 * <p>重投任务关掉的是 outbox 唯一剩下的那个窗口 —— <b>业务事务已提交，但消息还没投出去时进程挂了</b>。
 * 没有它，那条消息就永远不会发出去，coupon 和 MinIO 里的数据变成永久孤儿。</p>
 */
@Slf4j
@Component
public class MqMessageResendTask {

    /** 一轮最多重投多少条，避免积压时一次性把内存和 broker 打满 */
    private static final int RESEND_BATCH = 100;

    /** 已发送成功的记录保留天数，之后清掉，避免表无限增长 */
    private static final int RETENTION_DAYS = 7;

    private final MqMessageSender mqMessageSender;
    private final MqMessageService mqMessageService;

    public MqMessageResendTask(MqMessageSender mqMessageSender, MqMessageService mqMessageService) {
        this.mqMessageSender = mqMessageSender;
        this.mqMessageService = mqMessageService;
    }

    /**
     * 每分钟重投一轮。
     *
     * <p>{@code initialDelay} 给 1 分钟，避开启动期。多实例部署时每个实例都会跑这个任务、
     * 重复投同一批消息 —— 重复投递本身无害（消费端幂等），所以这里不加分布式锁；
     * 真要收敛，再加一把 Redis 锁即可。</p>
     */
    @Scheduled(fixedDelay = 60_000, initialDelay = 60_000)
    public void resendPending() {
        try {
            int count = mqMessageSender.resendPending(RESEND_BATCH);
            if (count > 0) {
                log.info("重投本地消息表的待投递消息 {} 条", count);
            }
        } catch (Exception e) {
            // 定时任务抛异常会被 Spring 的默认 ErrorHandler 打日志后吞掉，这里显式接住，
            // 保证下一轮照常执行，并且失败信息出现在一个固定的地方
            log.error("重投本地消息失败", e);
        }
    }

    /** 每天凌晨 3:30 清理已发送成功的旧记录 */
    @Scheduled(cron = "0 30 3 * * ?")
    public void cleanSentMessages() {
        try {
            Date deadline = Date.from(LocalDate.now()
                    .minusDays(RETENTION_DAYS)
                    .atStartOfDay(ZoneId.systemDefault())
                    .toInstant());
            int removed = mqMessageService.removeSentBefore(deadline);
            if (removed > 0) {
                log.info("清理本地消息表已发送记录 {} 条（{} 天前）", removed, RETENTION_DAYS);
            }
        } catch (Exception e) {
            log.error("清理本地消息表失败", e);
        }
    }
}
