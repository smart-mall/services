package coupon.service;

import com.baomidou.mybatisplus.extension.service.IService;
import common.vo.PageVO;
import coupon.entity.CouponSpuCategoryRelationEntity;

import java.util.Map;

import common.query.PageQuery;
/**
 * 优惠券分类关联
 *
 * @author ??
 * @email sunlightcs@gmail.com
 * @date 2025-09-15 11:10:43
 */
public interface CouponSpuCategoryRelationService extends IService<CouponSpuCategoryRelationEntity> {

    PageVO<CouponSpuCategoryRelationEntity> queryPage(PageQuery query);
}

