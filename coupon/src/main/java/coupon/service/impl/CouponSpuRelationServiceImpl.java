package coupon.service.impl;

import org.springframework.stereotype.Service;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import common.vo.PageVO;

import coupon.dao.CouponSpuRelationDao;
import coupon.entity.CouponSpuRelationEntity;
import coupon.service.CouponSpuRelationService;


import common.query.PageQuery;
@Service("couponSpuRelationService")
public class CouponSpuRelationServiceImpl extends ServiceImpl<CouponSpuRelationDao, CouponSpuRelationEntity> implements CouponSpuRelationService {

    @Override
    public PageVO<CouponSpuRelationEntity> queryPage(PageQuery query) {
        IPage<CouponSpuRelationEntity> page = this.page(
                query.toPage(),
                new QueryWrapper<CouponSpuRelationEntity>()
        );

        return new PageVO<>(page.getTotal(), page.getRecords());
    }

}