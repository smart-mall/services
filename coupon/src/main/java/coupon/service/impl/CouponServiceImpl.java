package coupon.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import common.vo.PageVO;
import coupon.dao.CouponDao;
import coupon.entity.CouponEntity;
import coupon.service.CouponService;
import org.springframework.stereotype.Service;



import common.query.KeyPageQuery;
/**
 * 优惠券分页查询实现，{@code key} 非空时同时模糊匹配券名与券 ID。
 *
 * <p>无状态，线程安全；单表分页由 MyBatis-Plus 的 {@link ServiceImpl} 提供。
 */
@Service("couponService")
public class CouponServiceImpl extends ServiceImpl<CouponDao, CouponEntity> implements CouponService {

    /** {@inheritDoc} */
    @Override
    public PageVO<CouponEntity> queryPage(KeyPageQuery query) {
        String key = query.getKey();
        LambdaQueryWrapper<CouponEntity> wrapper = new LambdaQueryWrapper<>();

        if (key != null && !key.isEmpty()) {
            wrapper.like(CouponEntity::getCouponName, key)
                    .or()
                    .like(CouponEntity::getId, key);
        }

        IPage<CouponEntity> page = this.page(
                query.toPage(),
                wrapper
        );

        return new PageVO<>(page.getTotal(), page.getRecords());
    }

}