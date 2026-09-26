package coupon.service;

import com.baomidou.mybatisplus.extension.service.IService;
import common.vo.PageVO;
import coupon.entity.CouponSpuRelationEntity;

import java.util.Map;

/**
 * 优惠券与产品关联
 *
 * @author ??
 * @email sunlightcs@gmail.com
 * @date 2025-09-15 11:10:43
 */
public interface CouponSpuRelationService extends IService<CouponSpuRelationEntity> {

    PageVO<CouponSpuRelationEntity> queryPage(Map<String, Object> params);
}

