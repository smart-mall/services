package es;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * SKU 在 Elasticsearch 中的模型
 * 对应 product 索引的映射结构
 */
@Data
public class SkuEsModel {

    /**
     * SKU ID
     */
    private Long skuId;

    /**
     * SPU ID
     */
    private Long spuId;

    /**
     * SKU 标题
     */
    private String skuTitle;

    /**
     * SKU 价格
     */
    private BigDecimal skuPrice;

    /**
     * SKU 图片
     */
    private String skuImg;

    /**
     * 销量
     */
    private Long saleCount;

    /**
     * 是否有库存
     */
    private Boolean hasStock;

    /**
     * 热度评分
     */
    private Long hotScore;

    /**
     * 品牌ID
     */
    private Long brandId;

    /**
     * 分类ID
     */
    private Long catalogId;

    /**
     * 品牌名称
     */
    private String brandName;

    /**
     * 品牌图片
     */
    private String brandImg;

    /**
     * 分类名称
     */
    private String catalogName;

    /**
     * 属性列表（嵌套类型）
     */
    private List<Attrs> attrs;

    /**
     * 商品属性（嵌套对象）
     */
    @Data
    public static class Attrs {
        /**
         * 属性ID
         */
        private Long attrId;

        /**
         * 属性名称
         */
        private String attrName;

        /**
         * 属性值
         */
        private String attrValue;
    }
}