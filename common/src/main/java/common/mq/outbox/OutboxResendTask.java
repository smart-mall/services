package common.mq.outbox;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

/**
 * 本地消息表的兜底任务：定时重投待投递消息，并清理过期的已发送记录。
 *
 * <p>在投递链路里位于末端：{@link OutboxPublisher} 首次投递失败的消息由它重投，是至少一次语义的最后保障。
 *
 * <p>多实例安全：重投前由 {@link OutboxStore#claim} 做 CAS 抢占，同一条消息不会被两个实例同时投。
 */
@Slf4j
public class OutboxResendTask {

    /** 单轮重投条数上限：一轮只取这么多候选，避免扫描与投递拖长定时任务的执行时间。 */
    private static final int RESEND_BATCH = 100;
    /** 已发送记录的保留天数：清理时删除最后更新时间早于该天数的 SENT 记录，留出排查窗口。 */
    private static final int RETENTION_DAYS = 7;

    private final OutboxPublisher outboxPublisher;
    private final OutboxStore outboxStore;

    public OutboxResendTask(OutboxPublisher outboxPublisher, OutboxStore outboxStore) {
        this.outboxPublisher = outboxPublisher;
        this.outboxStore = outboxStore;
    }

    /**
     * 扫描并重投待投递 / 错误消息。
     *
     * <p>每轮最多处理 {@value #RESEND_BATCH} 条，剩下的等下一轮；单轮失败只记日志，不影响后续轮次。
     */
    // 用 fixedDelay 而不是 fixedRate：一轮没跑完时不会叠加触发
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

    /**
     * 清理过期的已发送记录。
     *
     * <p>只删 SENT 且最后更新时间早于保留期的记录，待重投的消息不受影响。
     */
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
