package member.enums;

import lombok.Getter;

/**
 * 积分变动类型，决定一次积分增减落在流水表里的 {@code change_type} 取值与增减方向。
 *
 * <p>取值只增不改：已落库的流水行按 code 反查类型，改 code 会让历史行查不到类型。
 * 非订单驱动的变动（如管理员手工调整）不取这里的值，{@code changeType} 留 {@code null}。
 */
@Getter
public enum IntegrationChangeTypeEnum {

    /** 下单抵扣：扣掉本单使用的积分，余额减少。 */
    ORDER_USE(1, "下单抵扣", false),

    /** 关单退回：订单取消或超时关闭时把抵扣掉的积分还回余额，余额增加。 */
    ORDER_REFUND(2, "关单退回", true),

    /** 支付赠送：订单支付成功后按订单上的可得积分入账，余额增加。 */
    ORDER_GRANT(3, "支付赠送", true),
    ;

    /** 落库取值。 */
    private final int code;
    /** 说明文案，会员端流水列表直接显示。 */
    private final String msg;
    /** 是否让余额增加；{@code false} 表示扣减。 */
    private final boolean increase;

    /**
     * 构造一个变动类型。
     *
     * @param code 落库取值
     * @param msg 说明文案
     * @param increase 是否让余额增加
     */
    IntegrationChangeTypeEnum(int code, String msg, boolean increase) {
        this.code = code;
        this.msg = msg;
        this.increase = increase;
    }

    /**
     * 按落库取值反查类型。
     *
     * @param code 落库取值，可以为 {@code null}
     * @return 对应的类型；{@code code} 为 {@code null} 或不是本枚举的取值时返回 {@code null}
     */
    public static IntegrationChangeTypeEnum of(Integer code) {
        if (code == null) {
            return null;
        }
        for (IntegrationChangeTypeEnum type : values()) {
            if (type.code == code) {
                return type;
            }
        }
        return null;
    }
}
