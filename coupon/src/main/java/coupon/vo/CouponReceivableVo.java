package coupon.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 一张可领取券，由 coupon 组装后下发。两个出口共用：会员端券中心的「可领取」一档，
 * 以及管理端「送券」弹窗的候选列表（走 {@code CouponService#listGrantable}，那边另把已达限领的排掉）。
 *
 * <p>只带挑券要看的字段与两个数量：{@code remainCount} 决定按钮是"立即领取"还是"已领完"，
 * {@code receivedCount} 与 {@code perLimit} 一起决定是"立即领取"还是"已领取"。
 * 发行总量、发布开关这些管理端口径的字段不给前端。
 */
@Data
public class CouponReceivableVo {

    /** 券模板主键，领券时回传它。 */
    private Long couponId;

    /** 券名称。 */
    private String couponName;

    /** 券面金额。 */
    private BigDecimal amount;

    /** 使用门槛。 */
    private BigDecimal minPoint;

    /** 适用范围[0->全场通用；1->指定分类；2->指定商品]。 */
    private Integer useType;

    /** 券的可使用结束时间。 */
    private Date endTime;

    /** 领取截止时间，用于提示"即将结束领取"。 */
    private Date enableEndTime;

    /** 剩余可领张数。 */
    private Integer remainCount;

    /** 当前会员已领张数。 */
    private Integer receivedCount;

    /** 每人限领张数。 */
    private Integer perLimit;

}
