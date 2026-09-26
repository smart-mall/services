package coupon.service.impl;

import org.springframework.stereotype.Service;
import java.util.Map;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import common.vo.PageVO;

import coupon.dao.CouponSpuCategoryRelationDao;
import coupon.entity.CouponSpuCategoryRelationEntity;
import coupon.service.CouponSpuCategoryRelationService;


import common.query.PageQuery;
/**
 * 优惠券与商品分类关联的分页查询实现，不带筛选条件，直接按分页参数取单表数据。
 *
 * <p>无状态，线程安全；单表分页由 MyBatis-Plus 的 {@link ServiceImpl} 提供。
 */
@Service("couponSpuCategoryRelationService")
public class CouponSpuCategoryRelationServiceImpl extends ServiceImpl<CouponSpuCategoryRelationDao, CouponSpuCategoryRelationEntity> implements CouponSpuCategoryRelationService {

    /** {@inheritDoc} */
    @Override
    public PageVO<CouponSpuCategoryRelationEntity> queryPage(PageQuery query) {
        IPage<CouponSpuCategoryRelationEntity> page = this.page(query.toPage());

        return new PageVO<>(page.getTotal(), page.getRecords());
    }

}