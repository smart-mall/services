package coupon.vo;

import common.query.PageQuery;
import lombok.Getter;
import lombok.Setter;

/**
 * 领取记录的查询条件，对应 {@code GET /coupon/couponhistory/list}。
 *
 * <p>两个条件都可不传：管理端的全局发放记录页只翻页，券详情里的抽屉按 {@code couponId} 收窄。
 */
@Getter
@Setter
public class CouponHistoryPageQuery extends PageQuery {

    /** 按优惠券主键筛选；不传则返回全部券的领取记录。 */
    private Long couponId;

    /** 按会员主键筛选；不传则返回全部会员的领取记录。 */
    private Long memberId;
}
