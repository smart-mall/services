package order.task;

import lombok.extern.slf4j.Slf4j;
import order.service.OrderService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 订单自动确认收货任务：把已发货且超过 {@code auto_confirm_day} 天仍未确认的订单置为已完成。
 *
 * <p>cron 为 {@code 0 0/5 * * * ?}（字段顺序：秒 分 时 日 月 周），即每分钟的第 0 秒、每 5 分钟触发一次。
 * 间隔取分钟级是演示口径：自动确认期限本身是天级的，真实部署下扫描频率取小时级即可，
 * 这里缩短是为了让"到期即确认"能在本地被验证到。
 *
 * <p>不加分布式锁：确认是对每行做带状态条件的更新，多个实例同时跑只是重复扫一遍，
 * 既不会把订单置成错误状态，也不会与会员的手动确认互相覆盖。
 *
 * <p>本类无状态、线程安全。触发线程由 Spring 的调度线程池提供，与处理请求的线程不是同一个。
 */
@Slf4j
@Component
public class OrderAutoConfirmTask {

    /** 单轮确认条数上限：一轮只取这么多候选，控制单轮执行时间。 */
    private static final int CONFIRM_BATCH = 100;

    private final OrderService orderService;

    /**
     * 注入订单服务。
     *
     * @param orderService 订单服务，提供自动确认入口
     */
    public OrderAutoConfirmTask(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * 触发一次自动确认。
     *
     * <p>异常在此捕获并记日志，不向外传播：抛出去只会让调度线程打一条栈，下次触发照常，
     * 没有必要中断整轮调度。
     */
    @Scheduled(cron = "0 0/5 * * * ?")
    public void autoConfirm() {
        try {
            int confirmed = orderService.autoConfirmReceive(CONFIRM_BATCH);
            if (confirmed > 0) {
                log.info("自动确认收货完成，置为已完成 {} 单", confirmed);
            }
        } catch (Exception e) {
            log.error("自动确认收货失败，等下一轮重试", e);
        }
    }

}
