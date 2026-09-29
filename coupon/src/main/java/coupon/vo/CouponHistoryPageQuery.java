package coupon.vo;

import common.query.PageQuery;
import lombok.Getter;
import lombok.Setter;

/**
 * 领取记录的查询条件，对应管理端的 {@code GET /coupon/couponhistory/list}
 * 与会员端的 {@code GET /coupon/front/jwt/myCoupons}。
 *
 * <p>三个条件都可不传：管理端的全局发放记录页只翻页，券详情里的抽屉按 {@code couponId} 收窄，
 * 会员端的"我的券"按 {@code useType} 分成未使用 / 已使用 / 已过期三档。
 */
@Getter
@Setter
public class CouponHistoryPageQuery extends PageQuery {

    /** 按优惠券主键筛选；不传则返回全部券的领取记录。 */
    private Long couponId;

    /** 按会员主键筛选；不传则返回全部会员的领取记录。会员端由服务端强制填当前会员，不看入参。 */
    private Long memberId;

    /** 按使用状态筛选，取值见 {@code CouponUseStatusEnum}；不传则不限状态。 */
    private Integer useType;
}
