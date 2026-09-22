package coupon.service.impl;

import coupon.service.ProductCleanupService;
import coupon.service.SkuFullReductionService;
import coupon.service.SpuBoundsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
public class ProductCleanupServiceImpl implements ProductCleanupService {

    private final SpuBoundsService spuBoundsService;
    private final SkuFullReductionService skuFullReductionService;

    public ProductCleanupServiceImpl(SpuBoundsService spuBoundsService,
                                     SkuFullReductionService skuFullReductionService) {
        this.spuBoundsService = spuBoundsService;
        this.skuFullReductionService = skuFullReductionService;
    }

    @Override
    @Transactional
    public void cleanupProduct(List<Long> spuIds, List<Long> skuIds) {
        // 两个方法内部都是"空集合直接返回"，所以这里不用额外判空。
        // 都是纯 DELETE，天然幂等 —— 消息重复投递不会出问题，这也是不做去重表的原因。
        spuBoundsService.deleteBySpuIds(spuIds);
        skuFullReductionService.deleteBySkuIds(skuIds);
    }
}
