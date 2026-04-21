package coupon.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import common.utils.PageUtils;
import common.utils.Query;
import coupon.dao.SeckillSkuRelationDao;
import coupon.entity.SeckillSkuRelationEntity;
import coupon.service.SeckillSkuRelationService;
import org.springframework.stereotype.Service;
import org.thymeleaf.util.StringUtils;

import java.util.Map;


@Service("seckillSkuRelationService")
public class SeckillSkuRelationServiceImpl extends ServiceImpl<SeckillSkuRelationDao, SeckillSkuRelationEntity> implements SeckillSkuRelationService {

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        LambdaQueryWrapper<SeckillSkuRelationEntity> queryWrapper = new LambdaQueryWrapper<>();
        String promotionSessionId = (String)params.get("promotionSessionId");
        if (!StringUtils.isEmpty(promotionSessionId)) {
            queryWrapper.eq(SeckillSkuRelationEntity::getPromotionSessionId, promotionSessionId);
        }


        IPage<SeckillSkuRelationEntity> page = this.page(
                new Query<SeckillSkuRelationEntity>().getPage(params),
                queryWrapper
        );

        return new PageUtils(page);
    }

}