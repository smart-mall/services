package coupon.enums;

/**
 * 优惠券领取记录的使用状态，对应 {@code sms_coupon_history.use_type}。
 *
 * <p>它是券在会员手里的一生：领取时落 {@link #UNUSED}，下单随订单锁定时落 {@link #OCCUPIED}，
 * 付款后落 {@link #USED}，订单取消或超时关单时退回 {@link #UNUSED}，超过有效期由过期任务落 {@link #EXPIRED}。
 *
 * <p>注意与 {@code CouponEntity.useType} 同名不同义：那个是"这张券能买什么"的适用范围。
 *
 * <p>无状态、线程安全。
 */
public enum CouponUseStatusEnum {

    /** 未使用：已领到手，可用于下单。 */
    UNUSED(0, "未使用"),
    /** 已使用：已随某张订单付款核销。 */
    USED(1, "已使用"),
    /** 已过期：超过券的有效期，由过期任务或使用前的校验落定。 */
    EXPIRED(2, "已过期"),
    /** 占用中：已随某张订单锁定，订单尚未付款。此状态下不能再用于别的订单。 */
    OCCUPIED(3, "占用中");

    /** 落库取值。 */
    private final int code;

    /** 状态文案，直接展示在会员端"我的券"与管理端领取记录。 */
    private final String msg;

    CouponUseStatusEnum(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    /**
     * 获取落库取值。
     *
     * @return 状态码
     */
    public int getCode() {
        return code;
    }

    /**
     * 获取状态文案。
     *
     * @return 状态文案，不会为 {@code null}
     */
    public String getMsg() {
        return msg;
    }

    /**
     * 把落库取值翻成状态文案。
     *
     * <p>取不到时返回空串而不是抛异常：库里允许为 {@code NULL}，展示层不该因为一个空字段就整页报错。
     *
     * @param code 落库取值，可以为 {@code null}
     * @return 状态文案；{@code code} 为 {@code null} 或不在取值范围内时返回空串
     */
    public static String textOf(Integer code) {
        if (code == null) {
            return "";
        }
        for (CouponUseStatusEnum status : values()) {
            if (status.code == code) {
                return status.msg;
            }
        }
        return "";
    }

}
