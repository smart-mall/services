package order.enums;

import lombok.extern.slf4j.Slf4j;

/**
 * 售后申请状态枚举，{@code code} 落库到 {@code oms_order_return_apply.status}，
 * {@code msg} 作为文案返回前端。
 *
 * <p>取值只有一套，两条链各走各的路径（类型见 {@link AfterSaleTypeEnum}）：
 * <ul>
 *   <li><b>仅退款</b>：{@link #PENDING} →（平台退款成功）{@link #FINISHED}，
 *       或 {[@link #PENDING} →（后台拒绝）{@link #REJECTED}。不经过 {@link #RETURNING} ——
 *       货没发出去，没有"退货中"这一段。</li>
 *   <li><b>退货退款</b>：{@link #PENDING} →（审核通过）{@link #RETURNING}
 *       →（商家确认收到货、平台退款成功）{@link #FINISHED}，或 {@link #PENDING} → {@link #REJECTED}。</li>
 * </ul>
 *
 * <p>{@link #RETURNING} 不是装饰：它表示"已经同意退了、等会员把货寄回来"，这一段的长度由会员与快递决定，
 * 商家只有真收到货才会推进到 {@link #FINISHED}。
 *
 * <p><b>被拒绝后可以重新申请</b>：拒绝常常是因为凭证不清楚或描述不全，会员补齐后应当能再提交一次。
 * 所以"同一订单项不能重复申请"这条规则判的是"有没有正处于待处理/退货中/已完成的申请"，
 * 已拒绝的那条不算占用名额。
 */
@Slf4j
public enum AfterSaleStatusEnum {

    /** 待处理：会员提交申请后的初始状态，等待后台审核，取值 0。 */
    PENDING(0, "待处理"),

    /** 退货中：退货申请审核通过，已下发商家退货地址，等待会员寄回，取值 1。 */
    RETURNING(1, "退货中"),

    /** 已完成：款已退（退货的单表示商家已收货且已退款），取值 2。 */
    FINISHED(2, "已完成"),

    /** 已拒绝：后台驳回申请，理由写在处理备注里，取值 3。 */
    REJECTED(3, "已拒绝"),
    ;

    /** 状态码，落库到 {@code oms_order_return_apply.status}。 */
    private final Integer code;

    /** 状态文案，展示用。 */
    private final String msg;

    AfterSaleStatusEnum(Integer code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public Integer getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }

    /**
     * 取状态文案。
     *
     * <p>与订单状态文案同源的处理：库里的取值只由本枚举写入，取不到说明枚举漏了一档，
     * 不返回 {@code null} 以免前端多一次判空。</p>
     *
     * @param code 状态码，允许为 {@code null}
     * @return 对应文案；{@code null} 或未知码返回空串，未知码另外记一条 warn
     */
    public static String textOf(Integer code) {
        if (code == null) {
            return "";
        }
        for (AfterSaleStatusEnum value : values()) {
            if (value.code.equals(code)) {
                return value.msg;
            }
        }
        log.warn("未知的售后状态码：{}", code);
        return "";
    }
}
