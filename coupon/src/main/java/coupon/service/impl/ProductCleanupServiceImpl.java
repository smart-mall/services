package coupon.service.impl;

import coupon.service.ProductCleanupService;
import coupon.service.ProductReferenceService;
import coupon.service.SkuFullReductionService;
import coupon.service.SpuBoundsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 商品删除事件在 coupon 侧的清理执行方，一次删掉该商品在优惠库里的积分设置与满减、阶梯价、会员价，
 * 以及它留下的失效引用（已过使用期的券的范围行、已结束的场次关联行）。
 *
 * <p>链路位置：product 删除商品行后投递 {@code product.deleted}，{@code ProductDeletedListener} 消费后
 * 调用本类；待清理的数据都在本库，不需要回调 product。
 *
 * <p><b>仍然有效的引用不归本类管</b>：券还在使用期内、场次还没结束时，商品根本删不掉 ——
 * {@code product} 删除前已经同步问过 coupon，那一步就把删除拦住了。
 *
 * <p>无状态，线程安全。
 */
@Slf4j
@Service
public class ProductCleanupServiceImpl implements ProductCleanupService {

    private final SpuBoundsService spuBoundsService;

    private final SkuFullReductionService skuFullReductionService;

    private final ProductReferenceService productReferenceService;

    /**
     * 创建清理服务实例，注入两个清理入口与失效引用清理入口。
     *
     * @param spuBoundsService SPU 积分设置清理入口
     * @param skuFullReductionService 满减、阶梯价与会员价清理入口
     * @param productReferenceService 券范围与秒杀关联的失效引用清理入口
     */
    public ProductCleanupServiceImpl(SpuBoundsService spuBoundsService,
                                     SkuFullReductionService skuFullReductionService,
                                     ProductReferenceService productReferenceService) {
        this.spuBoundsService = spuBoundsService;
        this.skuFullReductionService = skuFullReductionService;
        this.productReferenceService = productReferenceService;
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public void cleanupProduct(List<Long> spuIds, List<Long> skuIds) {
        // 两个方法内部都是"空集合直接返回"，所以这里不用额外判空。
        // 都是纯 DELETE，天然幂等 —— 消息重复投递不会出问题，这也是不做去重表的原因。
        spuBoundsService.deleteBySpuIds(spuIds);
        skuFullReductionService.deleteBySkuIds(skuIds);
        // 失效引用：已过使用期的券的范围行、已结束（或场次已不存在）的秒杀关联行。
        // 仍然有效的一行都不删 —— 商品能删掉的前提就是没有它们，守卫那一步已经拦过。
        productReferenceService.deleteDeadReferences(spuIds, skuIds);
    }
}
