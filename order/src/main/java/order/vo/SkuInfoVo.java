package order.vo;

import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.math.BigDecimal;

/**
 * SKU 基本信息，由 product 服务经 Feign 提供，下单时用于校验商品是否仍存在。
 */
@Data
public class SkuInfoVo {

    /** SKU 标识。 */
    @TableId
    private Long skuId;

    /** 所属 SPU 标识。 */
    private Long spuId;

    /** SKU 名称。 */
    private String skuName;

    /** SKU 介绍描述。 */
    private String skuDesc;

    /** 所属分类 ID。 */
    private Long catalogId;

    /** 品牌 ID。 */
    private Long brandId;

    /** 默认展示图片地址。 */
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
