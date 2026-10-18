package coupon.vo;

import common.query.PageQuery;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

/**
 * 优惠券列表的查询条件，对应管理端的 {@code GET /coupon/coupon/list}。
 *
 * <p>所有条件都可不传，不传即不按该维度收窄：{@code key} 模糊匹配券名或券 ID，其余都是精确匹配，
 * 两个时间边界按"区间有交集"判定。
 *
 * <p>{@code status} 取 {@link coupon.enums.CouponStatusEnum} 的枚举名（{@code DRAFT}、
 * {@code NOT_STARTED}、{@code RECEIVING}、{@code SOLD_OUT}、{@code REVOKED}、{@code ENDED}）。
 * 券状态不落库、由发布开关与领取窗口现算，所以它不能像其它字段那样直接拼一个 {@code eq}，
 * 翻译方式见 {@code CouponServiceImpl#applyStatusFilter}。
 */
@Getter
@Setter
public class CouponPageQuery extends PageQuery {

    /** 关键字，模糊匹配券名或券 ID；不传则不限。 */
    private String key;

    /** 券类型，取值 0~3，含义见 {@code sms_coupon.coupon_type} 的列注释；不传则不限。 */
    private Integer couponType;

    /** 适用范围，取值 0~2，含义见 {@code CouponScopeEnum}；不传则不限。 */
    private Integer useType;

    /** 可领取的会员等级门槛，0 表示不限等级；不传则不限。 */
    private Integer memberLevel;

    /** 发布状态，0 未发布、1 已发布；不传则不限。 */
    private Integer publish;

    /** 派生状态，取 {@code CouponStatusEnum} 的枚举名；不传或取值非法时不限。 */
    private String status;

    /**
     * 领取窗口的时间下界，与券的领取区间求交集；不传则不限。
     *
     * <p>必须显式标 {@code @DateTimeFormat}：全局 Jackson 的 {@code date-format} 只作用于 JSON
     * 请求体，查询参数走的是 Spring 的转换服务，不标就按默认格式解析、收到 {@code 10001 参数格式校验失败}。
     * 格式与前端日期选择器的 {@code value-format} 一致。
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date enableStartFrom;

    /**
     * 领取窗口的时间上界，与券的领取区间求交集；不传则不限。
     *
     * @see #enableStartFrom
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date enableStartTo;

}
