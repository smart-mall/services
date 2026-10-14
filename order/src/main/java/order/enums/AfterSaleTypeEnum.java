package order.enums;

import lombok.extern.slf4j.Slf4j;

/**
 * 售后类型枚举，{@code code} 落库到 {@code oms_order_return_apply.type}，{@code msg} 作为文案返回前端。
 *
 * <p><b>为什么"仅退款"与"退货退款"是两条链，却共用一张表：</b>两者的触发时点与步骤都不同 ——
 * 仅退款发生在"已付款、还没发货"时，货没出去，退的是整单的钱，平台退完款这一单就作废；
 * 退货退款发生在"确认收货"之后，货已经在会员手里，必须先寄回来、商家收到了才退钱，退的是某一件商品的钱。
 * 但两者的申请态是同一份东西：谁提的、哪张订单、退多少钱、什么原因、处理到哪一步、谁处理的。
 * 所以申请单共用 {@code oms_order_return_apply}，类型显式落这一列，状态流转据此分岔
 * （仅退款 0→2/3，退货 0→1→2/3，见 {@link AfterSaleStatusEnum}）。
 *
 * <p><b>为什么不靠"某个字段是不是 NULL"来区分：</b>仅退款的单里 {@code skuId}、退货数量、
 * 退货人与商家收货信息确实都是空的，但那些空值的含义是"不适用"，不是"这是另一种业务"。
 * 用"空值即类型"当约定，每个读这张表的人都得先知道那条隐含规则；多一列换掉一条口口相传的规则，划算。
 */
@Slf4j
public enum AfterSaleTypeEnum {

    /** 仅退款：整单退钱、不寄回商品，取值 0。 */
    REFUND(0, "仅退款"),

    /** 退货退款：按订单项寄回商品、商家收到后退钱，取值 1。 */
    RETURN(1, "退货退款"),
    ;

    /** 类型码，落库到 {@code oms_order_return_apply.type}。 */
    private final Integer code;

    /** 类型文案，展示用。 */
    private final String msg;

    AfterSaleTypeEnum(Integer code, String msg) {
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
     * 取类型文案。
     *
     * <p>库里的取值只应由本枚举写入，取不到说明枚举漏了一档。不返回 {@code null}：前端拿到
     * {@code null} 要么显示空白、要么得再判一次空。</p>
     *
     * @param code 类型码，允许为 {@code null}
     * @return 对应文案；{@code null} 或未知码返回空串，未知码另外记一条 warn
     */
    public static String textOf(Integer code) {
        if (code == null) {
            return "";
        }
        for (AfterSaleTypeEnum value : values()) {
            if (value.code.equals(code)) {
                return value.msg;
            }
        }
        log.warn("未知的售后类型码：{}", code);
        return "";
    }

    /**
     * 判断是不是"仅退款"。
     *
     * @param code 类型码，允许为 {@code null}
     * @return 等于 {@link #REFUND} 的取值为 {@code true}，其余（含 {@code null}）为 {@code false}
     */
    public static boolean isRefund(Integer code) {
        return REFUND.code.equals(code);
    }
}
