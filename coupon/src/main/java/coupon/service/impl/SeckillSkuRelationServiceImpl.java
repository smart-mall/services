package coupon.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import common.vo.PageVO;
import coupon.dao.SeckillSkuRelationDao;
import coupon.entity.SeckillSkuRelationEntity;
import coupon.service.SeckillOfflineService;
import coupon.service.SeckillSkuRelationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;


import coupon.vo.SeckillSkuRelationPageQuery;
/**
 * 秒杀商品关联的查询与删除实现：查询按场次过滤，删除先下架缓存再删行。
 *
 * <p>无状态，线程安全；单表分页由 MyBatis-Plus 的 {@link ServiceImpl} 提供。
 */
@Service("seckillSkuRelationService")
public class SeckillSkuRelationServiceImpl extends ServiceImpl<SeckillSkuRelationDao, SeckillSkuRelationEntity> implements SeckillSkuRelationService {

    private final SeckillOfflineService seckillOfflineService;

    /**
     * @param seckillOfflineService 秒杀下架通知服务，用于删除前摘掉缓存
     */
    public SeckillSkuRelationServiceImpl(SeckillOfflineService seckillOfflineService) {
        this.seckillOfflineService = seckillOfflineService;
    }

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

    /** {@inheritDoc} */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeRelations(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }

        // 下架通知要用场次 ID 与 SKU ID，必须在删行之前把记录读出来
        List<SeckillSkuRelationEntity> relations = listByIds(ids);
        seckillOfflineService.offlineSkus(relations);

        removeByIds(ids);
    }

}
