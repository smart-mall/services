package coupon.service;

import com.baomidou.mybatisplus.extension.service.IService;
import common.vo.PageVO;
import coupon.entity.CouponEntity;

import java.util.Map;

import common.query.KeyPageQuery;
/**
 * 优惠券信息。
 *
 * <p>继承 {@link IService}，通用增删改查由 MyBatis-Plus 提供；本接口只补管理端的分页查询。
 */
public interface CouponService extends IService<CouponEntity> {

    /**
     * 分页查询优惠券，{@code key} 同时模糊匹配券名与优惠券 ID。
     *
     * <p>{@code key} 为空时不加筛选条件，返回全部优惠券。
     *
     * @param query 分页参数与关键字，不能为 {@code null}；{@code key} 可以为 {@code null} 或空白
     * @return 分页结果，无数据时 {@code rows} 为空列表、{@code total} 为 0，不返回 {@code null}
     */
    PageVO<CouponEntity> queryPage(KeyPageQuery query);
}

