package coupon.service;

import com.baomidou.mybatisplus.extension.service.IService;
import common.vo.PageVO;
import coupon.entity.CouponSpuRelationEntity;

import java.util.Map;

import common.query.PageQuery;
/**
 * 优惠券与产品关联
 */
public interface CouponSpuRelationService extends IService<CouponSpuRelationEntity> {

    PageVO<CouponSpuRelationEntity> queryPage(PageQuery query);
}

