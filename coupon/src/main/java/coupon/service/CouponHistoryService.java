package coupon.service;

import com.baomidou.mybatisplus.extension.service.IService;
import common.vo.PageVO;
import coupon.entity.CouponHistoryEntity;

import java.util.Map;

import common.query.PageQuery;
/**
 * 优惠券领取历史记录
 *
 * @author ??
 * @email sunlightcs@gmail.com
 * @date 2025-09-15 11:10:43
 */
public interface CouponHistoryService extends IService<CouponHistoryEntity> {

    PageVO<CouponHistoryEntity> queryPage(PageQuery query);
}

