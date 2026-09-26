package coupon.service;

import com.baomidou.mybatisplus.extension.service.IService;
import common.vo.PageVO;
import coupon.entity.CouponEntity;

import java.util.Map;

import common.query.KeyPageQuery;
/**
 * 优惠券信息
 *
 * @author ??
 * @email sunlightcs@gmail.com
 * @date 2025-09-15 11:10:43
 */
public interface CouponService extends IService<CouponEntity> {

    PageVO<CouponEntity> queryPage(KeyPageQuery query);
}

