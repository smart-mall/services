package coupon.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import common.vo.PageVO;
import coupon.dao.CouponHistoryDao;
import coupon.entity.CouponHistoryEntity;
import coupon.service.CouponHistoryService;
import coupon.vo.CouponHistoryPageQuery;
import org.springframework.stereotype.Service;

/**
 * 优惠券领取记录分页查询实现，按券或会员收窄，结果按领取时间倒序。
 *
 * <p>无状态，线程安全；单表分页由 MyBatis-Plus 的 {@link ServiceImpl} 提供。
 */
@Service("couponHistoryService")
public class CouponHistoryServiceImpl extends ServiceImpl<CouponHistoryDao, CouponHistoryEntity> implements CouponHistoryService {

    /** {@inheritDoc} */
    @Override
    public PageVO<CouponHistoryEntity> queryPage(CouponHistoryPageQuery query) {
        LambdaQueryWrapper<CouponHistoryEntity> wrapper = new LambdaQueryWrapper<CouponHistoryEntity>()
                .eq(query.getCouponId() != null, CouponHistoryEntity::getCouponId, query.getCouponId())
                .eq(query.getMemberId() != null, CouponHistoryEntity::getMemberId, query.getMemberId())
                // 主键兜底排序：同一秒内领的多张券时间相同，只按时间排会让翻页结果不稳定
                .orderByDesc(CouponHistoryEntity::getCreateTime)
                .orderByDesc(CouponHistoryEntity::getId);

        IPage<CouponHistoryEntity> page = this.page(query.toPage(), wrapper);

        return new PageVO<>(page.getTotal(), page.getRecords());
    }

}
