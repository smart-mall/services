package common.mq.outbox;

/**
 * 本地消息表的投递状态。
 *
 * <p>{@link #SENT} 只代表 broker 已 ack，不代表消费端处理完了。
 */
public enum OutboxStatus {

    /** 待投递：已落库，等事务提交后投递，或被重投任务扫到。 */
    PENDING(0, "待投递"),
    /** 已发送：broker 已 ack，不再重投，由清理任务按保留期删除。 */
    SENT(1, "已发送"),
    /** 错误抵达：被 return（不可路由）或被 nack，等待重投。 */
    ERROR(2, "错误抵达"),
    /** 已抵达：消费端回执状态，当前链路没有写入方。 */
    ARRIVED(3, "已抵达"),
    /** 投递中：已被某个实例 CAS 抢占，其他实例在超时前不再处理。 */
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
