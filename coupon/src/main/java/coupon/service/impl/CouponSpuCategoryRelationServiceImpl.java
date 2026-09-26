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
@Service("couponSpuCategoryRelationService")
public class CouponSpuCategoryRelationServiceImpl extends ServiceImpl<CouponSpuCategoryRelationDao, CouponSpuCategoryRelationEntity> implements CouponSpuCategoryRelationService {

    @Override
    public PageVO<CouponSpuCategoryRelationEntity> queryPage(PageQuery query) {
        IPage<CouponSpuCategoryRelationEntity> page = this.page(query.toPage());

        return new PageVO<>(page.getTotal(), page.getRecords());
    }

}