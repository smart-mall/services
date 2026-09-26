package coupon.service;

import com.baomidou.mybatisplus.extension.service.IService;
import common.vo.PageVO;
import coupon.entity.CouponHistoryEntity;

import java.util.Map;

import common.query.PageQuery;
/**
 * 优惠券领取历史记录
 */
public interface CouponHistoryService extends IService<CouponHistoryEntity> {

    PageVO<CouponHistoryEntity> queryPage(PageQuery query);
}

