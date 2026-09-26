package es;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * SKU 在 Elasticsearch 中的文档模型，对应 {@code product} 索引的映射结构。
 */
@Data
public class SkuEsModel {

    /** SKU 主键。 */
    private Long skuId;

    /** 所属 SPU 主键。 */
    private Long spuId;

    /** SKU 标题。 */
    private String skuTitle;

    /** SKU 价格。 */
    private BigDecimal skuPrice;

    /** SKU 图片地址。 */
    private String skuImg;

    /** 销量。 */
    private Long saleCount;

    /** 是否有库存。 */
    private Boolean hasStock;

    /** 热度评分，参与搜索排序。 */
    private Long hotScore;

    /** 品牌主键。 */
    private Long brandId;

    /** 分类主键。 */
    private Long catalogId;

    /** 品牌名称。 */
    private String brandName;

    /** 品牌图片地址。 */
    private String brandImg;

    /** 分类名称。 */
    private String catalogName;

    /** 属性列表，在索引映射中是 nested 类型。 */
    private List<Attrs> attrs;

    /** SKU 的属性键值对，在索引映射中是 nested 子文档。 */
    @Data
    public static class Attrs {
        /** 属性主键。 */
        private Long attrId;

        /** 属性名称。 */
        private String attrName;

        /** 属性值。 */
        private String attrValue;
    }
}