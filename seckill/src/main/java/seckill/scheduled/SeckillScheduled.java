package seckill.scheduled;

import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import seckill.service.SeckillService;

import java.util.concurrent.TimeUnit;

/**
 * 秒杀商品定时上架任务：每天 0 点上架最近三天要参与的秒杀场次与商品。
 *
 * <p>cron 为 {@code 0 0 0 * * ?}（字段顺序：秒 分 时 日 月 周），即每天 00:00:00 触发，
 * 覆盖当天、明天、后天三个整天。
 *
 * <p>多实例部署时靠 Redis 分布式锁让上架串行执行，同一时刻只有一个实例真正在跑。
 */
@Slf4j
@Service
public class SeckillScheduled {

    private final SeckillService seckillService;

    private final RedissonClient redissonClient;

    /** 上架任务的分布式锁 key：多实例同时触发时让上架串行执行。 */
    private final String upload_lock = "seckill:upload:lock";

    public SeckillScheduled(SeckillService seckillService, RedissonClient redissonClient) {
        this.seckillService = seckillService;
        this.redissonClient = redissonClient;
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

        RLock lock = redissonClient.getLock(upload_lock);
        try {
            // 租期 10 秒：实例宕机后锁自动过期，不会让上架永久卡死
            lock.lock(10, TimeUnit.SECONDS);
            seckillService.uploadSeckillSkuLatest3Days();
        } catch (Exception e) {
            log.error("获取分布式锁失败...", e);
        } finally {
            // 必须在 finally 里解锁，上架抛异常时其它实例才拿得到锁
            lock.unlock();
        }
    }
}
