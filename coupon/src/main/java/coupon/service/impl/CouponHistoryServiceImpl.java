package coupon.service.impl;

import org.springframework.stereotype.Service;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import common.vo.PageVO;

import coupon.dao.CouponHistoryDao;
import coupon.entity.CouponHistoryEntity;
import coupon.service.CouponHistoryService;


import common.query.PageQuery;
@Service("couponHistoryService")
public class CouponHistoryServiceImpl extends ServiceImpl<CouponHistoryDao, CouponHistoryEntity> implements CouponHistoryService {

    @Override
    public PageVO<CouponHistoryEntity> queryPage(PageQuery query) {
        IPage<CouponHistoryEntity> page = this.page(
                query.toPage(),
                new QueryWrapper<CouponHistoryEntity>()
        );

        return new PageVO<>(page.getTotal(), page.getRecords());
    }

}