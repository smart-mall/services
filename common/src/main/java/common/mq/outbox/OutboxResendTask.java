package common.mq.outbox;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

/**
 * 本地消息表的兜底任务：重投 + 清理。
 */
@Slf4j
public class OutboxResendTask {

    private static final int RESEND_BATCH = 100;
    private static final int RETENTION_DAYS = 7;

    private final OutboxPublisher outboxPublisher;
    private final OutboxStore outboxStore;

    public OutboxResendTask(OutboxPublisher outboxPublisher, OutboxStore outboxStore) {
        this.outboxPublisher = outboxPublisher;
        this.outboxStore = outboxStore;
    }

    @Scheduled(fixedDelay = 60_000, initialDelay = 60_000)
    public void resendPending() {
        try {
            int count = outboxPublisher.resendPending(RESEND_BATCH);
            if (count > 0) {
                log.info("重投本地消息表的待投递消息 {} 条", count);
            }
        } catch (Exception e) {
            log.error("重投本地消息失败", e);
        }
    }

    @Scheduled(cron = "0 30 3 * * ?")
    public void cleanSentMessages() {
        try {
            Date deadline = Date.from(LocalDate.now()
                    .minusDays(RETENTION_DAYS)
                    .atStartOfDay(ZoneId.systemDefault())
                    .toInstant());
            int removed = outboxStore.removeSentBefore(deadline);
            if (removed > 0) {
                log.info("清理本地消息表已发送记录 {} 条（{} 天前）", removed, RETENTION_DAYS);
            }
        } catch (Exception e) {
            log.error("清理本地消息表失败", e);
        }
    }
}
