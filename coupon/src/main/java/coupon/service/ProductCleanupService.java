package coupon.service;

import java.util.List;

/**
 * 商品删除后清理 coupon 侧优惠数据的入口。
 *
 * <p>由 {@code ProductDeletedListener} 在收到 {@code product.deleted} 事件后调用：数据在本库，
 * 不需要跨服务调用。
 */
public interface ProductCleanupService {

    /**
     * 一次清掉四张表：{@code sms_spu_bounds}（按 spuId）以及 {@code sms_sku_ladder}、
     * {@code sms_sku_full_reduction}、{@code sms_member_price}（按 skuId）。
     *
     * <p>整批在一个事务里：只成功一半时，虽然消息重投后最终会修好，但中间这段时间数据不自洽。
     * 全是 DELETE，天然幂等，消息重复投递不会出错。
     *
     * @param spuIds 商品 SPU 标识集合；为 {@code null} 或空集合时对应表不做任何操作
     * @param skuIds 商品 SKU 标识集合；为 {@code null} 或空集合时对应表不做任何操作
     */
    void cleanupProduct(List<Long> spuIds, List<Long> skuIds);
}
