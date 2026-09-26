package coupon.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import common.vo.PageVO;
import common.utils.Query;
import coupon.dao.SeckillSkuRelationDao;
import coupon.entity.SeckillSkuRelationEntity;
import coupon.service.SeckillSkuRelationService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Map;


@Service("seckillSkuRelationService")
public class SeckillSkuRelationServiceImpl extends ServiceImpl<SeckillSkuRelationDao, SeckillSkuRelationEntity> implements SeckillSkuRelationService {

    @Override
    public PageVO<SeckillSkuRelationEntity> queryPage(Map<String, Object> params) {
        LambdaQueryWrapper<SeckillSkuRelationEntity> queryWrapper = new LambdaQueryWrapper<>();
        String promotionSessionId = (String)params.get("promotionSessionId");
        // 原来是 org.thymeleaf.util.StringUtils —— 前台迁到 Vue 之后模板和 thymeleaf 依赖都摘了，
        // 这里只是判空，换成 Spring 自己的就行。
        if (StringUtils.hasText(promotionSessionId)) {
            queryWrapper.eq(SeckillSkuRelationEntity::getPromotionSessionId, promotionSessionId);
        }


        IPage<SeckillSkuRelationEntity> page = this.page(
                new Query<SeckillSkuRelationEntity>().getPage(params),
                queryWrapper
        );

        return new PageVO<>(page.getTotal(), page.getRecords());
    }

}