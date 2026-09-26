package coupon.service;

import com.baomidou.mybatisplus.extension.service.IService;
import common.vo.PageVO;
import coupon.entity.CouponEntity;

import java.util.Map;

import common.query.KeyPageQuery;
/**
 * 优惠券信息
 */
public interface CouponService extends IService<CouponEntity> {

    PageVO<CouponEntity> queryPage(KeyPageQuery query);
}

