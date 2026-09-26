package cart.vo;

import lombok.Data;

import java.math.BigDecimal;



/**
 * 商品服务返回的 SKU 信息，购物车只取其中用于展示与计价的部分。
 */
@Data
public class SkuInfoVo {

    /** 商品 SKU 标识。 */
    private Long skuId;

    /** 所属 SPU 标识。 */
    private Long spuId;

    /** SKU 名称。 */
    private String skuName;

    /** SKU 介绍描述。 */
    private String skuDesc;

    /** 所属分类 ID。 */
    private Long catalogId;

    /** 所属品牌 ID。 */
    private Long brandId;

    /** 默认图片地址。 */
    private String skuDefaultImg;

    /** 标题。 */
    private String skuTitle;

    /** 副标题。 */
    private String skuSubtitle;

    /** 价格。 */
    private BigDecimal price;

    /** 销量。 */
    private Long saleCount;

}
