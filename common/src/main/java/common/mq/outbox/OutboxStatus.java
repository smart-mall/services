package common.mq.outbox;

/**
 * 本地消息表的状态。SENT 只代表 broker 收下了，不代表消费端处理完了。
 */
public enum OutboxStatus {

    /** 待投递 */
    PENDING(0, "待投递"),
    /** broker 已 ack，不再重投 */
    SENT(1, "已发送"),
    /** 被 return（不可路由）或被 nack，要重投 */
    ERROR(2, "错误抵达"),
    /** 消费端回执，本版未使用 */
    ARRIVED(3, "已抵达"),
    /** 重投任务已抢占，避免多实例重复投 */
    CLAIMED(4, "投递中"),
    ;

    private final int code;
    private final String msg;

    OutboxStatus(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public int getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }
}
