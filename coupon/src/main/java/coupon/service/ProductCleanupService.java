package coupon.service;

import java.util.List;

/**
 * 商品被删除后，清理 coupon 侧为它写入的优惠数据。
 *
 * <p>由 {@code ProductDeletedListener} 在收到 {@code product.deleted} 事件后调用，
 * 是<b>本地调用</b>而不是 Feign —— 数据就在自己库里，没有跨服务的必要。</p>
 */
public interface ProductCleanupService {

    /**
     * 一次清掉四张表：{@code sms_spu_bounds}（按 spuId）以及 {@code sms_sku_ladder}、
     * {@code sms_sku_full_reduction}、{@code sms_member_price}（按 skuId）。
     *
     * <p>整批在一个事务里，要么全成要么全不成 —— 分别成功一半的话，虽然消息会被重投从而
     * 最终修好，但中间那段时间数据是不自洽的。</p>
     */
    void cleanupProduct(List<Long> spuIds, List<Long> skuIds);
}
