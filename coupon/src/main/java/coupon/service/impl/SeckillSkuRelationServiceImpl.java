package coupon.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import common.vo.PageVO;
import coupon.dao.SeckillSkuRelationDao;
import coupon.entity.SeckillSkuRelationEntity;
import coupon.service.SeckillSkuRelationService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;



import coupon.vo.SeckillSkuRelationPageQuery;
/**
 * 秒杀商品关联的分页查询实现，{@code promotionSessionId} 非空时按场次过滤。
 *
 * <p>无状态，线程安全；单表分页由 MyBatis-Plus 的 {@link ServiceImpl} 提供。
 */
@Service("seckillSkuRelationService")
public class SeckillSkuRelationServiceImpl extends ServiceImpl<SeckillSkuRelationDao, SeckillSkuRelationEntity> implements SeckillSkuRelationService {

    /** {@inheritDoc} */
    @Override
    public PageVO<SeckillSkuRelationEntity> queryPage(SeckillSkuRelationPageQuery query) {
        LambdaQueryWrapper<SeckillSkuRelationEntity> queryWrapper = new LambdaQueryWrapper<>();
        String promotionSessionId = query.getPromotionSessionId();
        // 判空用 Spring 自带的 StringUtils：本模块依赖里没有 thymeleaf
        if (StringUtils.hasText(promotionSessionId)) {
            queryWrapper.eq(SeckillSkuRelationEntity::getPromotionSessionId, promotionSessionId);
        }


        IPage<SeckillSkuRelationEntity> page = this.page(
                query.toPage(),
                queryWrapper
        );

        return new PageVO<>(page.getTotal(), page.getRecords());
    }

}