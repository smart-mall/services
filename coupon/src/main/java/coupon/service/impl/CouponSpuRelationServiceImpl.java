package coupon.service.impl;

import org.springframework.stereotype.Service;
import java.util.Map;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import common.vo.PageVO;

import coupon.dao.CouponSpuRelationDao;
import coupon.entity.CouponSpuRelationEntity;
import coupon.service.CouponSpuRelationService;


import common.query.PageQuery;
/**
 * 优惠券与商品 SPU 关联的分页查询实现，不带筛选条件，直接按分页参数取单表数据。
 *
 * <p>无状态，线程安全；单表分页由 MyBatis-Plus 的 {@link ServiceImpl} 提供。
 */
@Service("couponSpuRelationService")
public class CouponSpuRelationServiceImpl extends ServiceImpl<CouponSpuRelationDao, CouponSpuRelationEntity> implements CouponSpuRelationService {

    /** {@inheritDoc} */
    @Override
    public PageVO<CouponSpuRelationEntity> queryPage(PageQuery query) {
        IPage<CouponSpuRelationEntity> page = this.page(query.toPage());

        return new PageVO<>(page.getTotal(), page.getRecords());
    }

}