package product.constant;

/**
 * 本地消息表（outbox）的状态。
 *
 * <p>语义上只有两件事需要区分：<b>还要不要再投一次</b>，以及<b>为什么没成功</b>。</p>
 *
 * <p>{@link #SENT} 只代表"broker 确认收下了"，不代表"消费端处理完了" —— 只要消息进了
 * broker，剩下的可靠性由 MQ 持久化 + 消费端 ack 接手。要做到"消费完成"得加消费端回执，
 * 成本高一个量级，所以 {@link #ARRIVED} 这一版不使用，只把字段留在表里。</p>
 */
public enum MqMessageStatus {

    /** 待投递。业务事务里插入时的初始状态，定时任务扫的就是它 */
    PENDING(0, "待投递"),
    /** broker 已 ack。不再重投 */
    SENT(1, "已发送"),
    /** 被 return（不可路由）或被 nack。要重投 + 告警 */
    ERROR(2, "错误抵达"),
    /** 消费端回执。本版未使用 */
    ARRIVED(3, "已抵达"),
    ;

    private final int code;
    private final String msg;

    MqMessageStatus(int code, String msg) {
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
