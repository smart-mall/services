package seckill.vo;

import lombok.Data;

import java.math.BigDecimal;



/**
 * SKU 基本信息，由 product 服务提供，上架时远程取回后嵌进秒杀缓存供前端展示。
 */
@Data
public class SkuInfoVo {

    /** 商品 SKU ID。 */
    private Long skuId;

    /** 所属 SPU ID。 */
    private Long spuId;

    /** SKU 名称。 */
    private String skuName;

    /** SKU 介绍描述。 */
    private String skuDesc;

    /** 所属分类 ID。 */
    private Long catalogId;

    /** 品牌 ID。 */
    private Long brandId;

    /** 默认图片地址。 */
    private String skuDefaultImg;

    /** 标题。 */
    private String skuTitle;

    /** 副标题。 */
    private String skuSubtitle;

    /** 商品原价，与秒杀价分开展示。 */
    private BigDecimal price;

    /** 销量。 */
    private Long saleCount;

}
