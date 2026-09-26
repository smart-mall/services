package search.service;

import es.SkuEsModel;

import java.util.List;

/**
 * 商品文档的 ES 写入服务：上架时批量索引 SKU 文档，下架时按 SPU 删除。
 */
public interface ProductSaveService {

    /**
     * 批量把上架商品的 SKU 文档写入 ES。
     *
     * @param skuEsModels 上架商品的 SKU 文档列表，不能为 {@code null}
     * @return {@code true} 表示全部写入成功；有任一条失败或 ES 未返回结果时返回 {@code false}
     */
    public boolean productStatusUp(List<SkuEsModel> skuEsModels);

    /**
     * 按下架商品的 SPU 标识删除 ES 中对应的 SKU 文档。
     *
     * <p>实现方必须保证：删除对后续检索立即可见，不能等 ES 的下一次刷新。
     *
     * @param spuIds 下架的 SPU 标识列表，允许为 {@code null} 或空
     * @return {@code true} 表示删除已执行；入参为空时直接返回 {@code true}
     */
    boolean productStatusDown(List<Long> spuIds);
}
