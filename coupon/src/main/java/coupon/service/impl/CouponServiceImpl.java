package coupon.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import common.utils.PageUtils;
import common.utils.Query;
import coupon.dao.CouponDao;
import coupon.entity.CouponEntity;
import coupon.service.CouponService;
import org.springframework.stereotype.Service;

import java.util.Map;


@Service("couponService")
public class CouponServiceImpl extends ServiceImpl<CouponDao, CouponEntity> implements CouponService {

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        String key = (String)params.get("key");
        LambdaQueryWrapper<CouponEntity> wrapper = new LambdaQueryWrapper<>();

        if (key != null && !key.isEmpty()) {
            wrapper.like(CouponEntity::getCouponName, key)
                    .or()
                    .like(CouponEntity::getId, key);
        }

        IPage<CouponEntity> page = this.page(
                new Query<CouponEntity>().getPage(params),
                wrapper
        );

        return new PageUtils(page);
    }

}