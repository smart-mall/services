package coupon.task;

import coupon.service.BirthdayGrantService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 生日赠券任务：给今天过生日的会员发一遍会员赠券。
 *
 * <p>cron 为 {@code 0 0/5 * * * ?}（字段顺序：秒 分 时 日 月 周），即每 5 分钟触发一次。
 * 间隔取分钟级是演示口径：生日本身是天级的，真实部署下每天扫一次即可，这里缩短是为了让
 * "到生日就发"能在本地被验证到；多发由"一年一张"的判重挡住。
 *
 * <p>不加分布式锁，与 {@code OrderAutoConfirmTask} 同源：当前部署每个服务一个实例，串行重复执行
 * 由"一年一张"的判重挡住，不会多发。与它的不同之处在后果 —— 那里重复扫只是白扫一遍，这里两个
 * 实例**同时**跑会各自查到"今年没发过"而各发一张，所以多实例部署时必须在这里加锁，
 * 或把"一年一张"落成唯一索引。
 *
 * <p>本类无状态、线程安全。触发线程由 Spring 的调度线程池提供，与处理请求的线程不是同一个。
 */
@Slf4j
@Component
public class BirthdayGrantTask {

    private final BirthdayGrantService birthdayGrantService;

    /**
     * 注入生日赠券服务。
     *
     * @param birthdayGrantService 生日赠券服务，提供发放入口
     */
    public BirthdayGrantTask(BirthdayGrantService birthdayGrantService) {
        this.birthdayGrantService = birthdayGrantService;
    }

    /**
     * 触发一次生日赠券。
     *
     * <p>异常在此捕获并记日志，不向外传播：抛出去只会让调度线程打一条栈，下一轮照常触发。
     */
    @Scheduled(cron = "0 0/5 * * * ?")
    public void grant() {
        try {
            int granted = birthdayGrantService.grantToday();
            if (granted > 0) {
                log.info("生日赠券完成，共发出 {} 张", granted);
            }
        } catch (Exception e) {
            log.error("生日赠券失败，等下一轮重试", e);
        }
    }

}
