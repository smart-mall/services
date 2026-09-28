package coupon.service;

import com.baomidou.mybatisplus.extension.service.IService;
import common.vo.PageVO;
import coupon.entity.CouponHistoryEntity;
import coupon.vo.CouponHistoryPageQuery;

/**
 * 优惠券领取记录。
 *
 * <p>继承 {@link IService}，通用增删改查由 MyBatis-Plus 提供；本接口只补管理端的分页查询。
 * 领取记录的正常写入方是领券与后台发券，管理端不需要直接增删改。
 */
public interface CouponHistoryService extends IService<CouponHistoryEntity> {

    /**
     * 分页查询优惠券领取记录，可按券或会员收窄。
     *
     * <p>实现方必须保证结果按领取时间倒序、同一时间下按主键倒序 —— 没有稳定序的分页
     * 会在翻页时重复或漏掉记录。
     *
     * @param query 分页参数与筛选条件，不能为 {@code null}；{@code couponId} / {@code memberId}
     *              可以为 {@code null}，为 {@code null} 时该条件不参与筛选
     * @return 分页结果，无数据时 {@code rows} 为空列表、{@code total} 为 0，不返回 {@code null}
     */
    PageVO<CouponHistoryEntity> queryPage(CouponHistoryPageQuery query);
}
