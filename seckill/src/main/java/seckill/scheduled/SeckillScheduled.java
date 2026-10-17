package seckill.scheduled;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import seckill.service.SeckillService;

/**
 * 秒杀商品定时上架任务：每天 0 点上架最近三天要参与的秒杀场次与商品。
 *
 * <p>cron 为 {@code 0 0 0 * * ?}（字段顺序：秒 分 时 日 月 周），即每天 00:00:00 触发，
 * 覆盖当天、明天、后天三个整天。
 *
 * <p>上架流程（含跨实例的分布式锁）由 {@link SeckillService#uploadSeckillSkuLatest3Days()} 承担，
 * 后台手动触发的也是同一个方法。
 */
@Slf4j
@Service
public class SeckillScheduled {

    private final SeckillService seckillService;

    /**
     * @param seckillService 秒杀业务服务，由容器注入
     */
    public SeckillScheduled(SeckillService seckillService) {
        this.seckillService = seckillService;
    }

    // TODO: 保证幂等性问题
    /**
     * 触发最近三天秒杀商品的上架。
     *
     * <p>上架过程中抛出的异常在此捕获并记日志，不会向外传播。
     */
    @Scheduled(cron = "0 0 0 * * ?")
    public void uploadSeckillSkuLatest3Days() {
        log.info("上架秒杀的商品...");

        try {
            seckillService.uploadSeckillSkuLatest3Days();
        } catch (Exception e) {
            log.error("秒杀上架失败", e);
        }
    }
}
