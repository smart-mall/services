package coupon.enums;

/**
 * 优惠券的适用范围，对应 {@code sms_coupon.use_type}。
 *
 * <p>{@link #ALL} 不需要关联明细；{@link #CATEGORY} 与 {@link #SPU} 各自依赖
 * {@code sms_coupon_spu_category_relation} 与 {@code sms_coupon_spu_relation} 里的明细。
 *
 * <p>注意与 {@code CouponHistoryEntity.useType} 同名不同义：那个是券在会员手里的使用状态。
 *
 * <p>无状态、线程安全。
 */
public enum CouponScopeEnum {

    /** 全场通用：整单金额都参与门槛比较。 */
    ALL(0, "全场通用"),
    /** 指定分类：只有属于所关联分类的商品参与。 */
    CATEGORY(1, "指定分类"),
    /** 指定商品：只有所关联 SPU 下的商品参与。 */
    SPU(2, "指定商品");

    /** 落库取值。 */
    private final int code;

    /** 适用范围文案。 */
    private final String msg;

    CouponScopeEnum(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    /**
     * 获取落库取值。
     *
     * @return 取值范围
     */
    public int getCode() {
        return code;
    }

    /**
     * 获取适用范围文案。
     *
     * @return 适用范围文案，不会为 {@code null}
     */
    public String getMsg() {
        return msg;
    }

    /**
     * 判断落库取值是否就是当前适用范围。
     *
     * <p>比较收在这里而不是散在调用处：{@code useType} 允许为 {@code null}，
     * 调用处直接拆箱比较会踩空指针，这里统一按"不是"处理。
     *
     * @param code 落库取值，可以为 {@code null}
     * @return {@code true} 表示与当前常量取值相同
     */
    public boolean matches(Integer code) {
        return code != null && code == this.code;
    }

}
