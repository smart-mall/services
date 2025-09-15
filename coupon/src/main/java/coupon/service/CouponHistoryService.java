package coupon.service;

import com.baomidou.mybatisplus.extension.service.IService;
import common.utils.PageUtils;
import coupon.entity.CouponHistoryEntity;

import java.util.Map;

/**
 * 优惠券领取历史记录
 *
 * @author ??
 * @email sunlightcs@gmail.com
 * @date 2025-09-15 11:10:43
 */
public interface CouponHistoryService extends IService<CouponHistoryEntity> {

    PageUtils queryPage(Map<String, Object> params);
}

