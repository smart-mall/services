package member.enums;

import lombok.Getter;

/**
 * 成长值变动类型，决定一次成长值增减落在流水表里的 {@code change_type} 取值。
 *
 * <p>取值只增不改：已落库的流水行按 code 反查类型，改 code 会让历史行查不到类型。
 * 非订单驱动的变动（如管理员手工调整）不取这里的值，{@code changeType} 留 {@code null}。
 *
 * <p>每一项都让成长值增大：成长值只有增加来源，没有扣减路径，等级因此只升不降。
 */
@Getter
public enum GrowthChangeTypeEnum {

    /** 下单赠送：订单支付成功后按订单上的可得成长值入账。 */
    ORDER_GRANT(1, "下单赠送"),

    /** 评价赠送：评价成功后按会员当前等级配置的每次评价成长值入账。 */
    COMMENT_GRANT(2, "评价赠送"),
    ;

    /** 落库取值。 */
    private final int code;
    /** 说明文案，会员端流水列表直接显示。 */
    private final String msg;

    /**
     * 构造一个变动类型。
     *
     * @param code 落库取值
     * @param msg 说明文案
     */
    GrowthChangeTypeEnum(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    /**
     * 按落库取值反查类型。
     *
     * @param code 落库取值，可以为 {@code null}
     * @return 对应的类型；{@code code} 为 {@code null} 或不是本枚举的取值时返回 {@code null}
     */
    public static GrowthChangeTypeEnum of(Integer code) {
        if (code == null) {
            return null;
        }
        for (GrowthChangeTypeEnum type : values()) {
            if (type.code == code) {
                return type;
            }
        }
        return null;
    }
}
