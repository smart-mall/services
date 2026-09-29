package coupon.task;

import coupon.service.CouponUseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 已领到手的券的过期清理任务：把超过券有效期的"未使用"券置为"已过期"。
 *
 * <p>cron 为 {@code 0 0 * * * ?}（字段顺序：秒 分 时 日 月 周），即每小时整点触发。
 * 券的有效期是精确到时刻的，任务只负责让"我的券"列表显示得准；能否使用由使用链路自己按时刻判定，
 * 所以两次任务之间过期的券也不会被用掉，间隔取小时级是显示精度与扫描成本之间的折中。
 *
 * <p>不加分布式锁：清理是对每行做带状态条件的更新，多个实例同时跑只是重复扫一遍，
 * 不会把券置成错误状态。秒杀上架那类任务需要锁，是因为它的操作不是幂等的。
 *
 * <p>本类无状态、线程安全。触发线程由 Spring 的调度线程池提供，与处理请求的线程不是同一个。
 */
@Slf4j
@Component
public class CouponExpireTask {

    private final CouponUseService couponUseService;

    /**
     * 注入券使用服务。
     *
     * @param couponUseService 券使用服务，提供过期清理入口
     */
    public CouponExpireTask(CouponUseService couponUseService) {
        this.couponUseService = couponUseService;
    }

    /**
     * 触发一次过期清理。
     *
     * <p>异常在此捕获并记日志，不向外传播：抛出去只会让调度线程打一条栈，
     * 下次触发照常，没有必要中断整轮调度。
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void expireCoupons() {
        try {
            int expired = couponUseService.expire();
            if (expired > 0) {
                log.info("优惠券过期清理完成，本次置为已过期 {} 张", expired);
            }
        } catch (Exception e) {
            log.error("优惠券过期清理失败，等下一轮重试", e);
        }
    }

}
