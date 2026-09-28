package order.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;


/**
 * SPU 基本信息，由 product 服务经 Feign 提供。
 */
@Data
public class SpuInfoVo {

    /** SPU 标识。 */
    private Long id;

    /** 商品名称。 */
    private String spuName;

    /** 商品描述。 */
    private String spuDescription;

    /** SPU 主图地址，product 从该 SPU 的图集里取一张回填。 */
    private String spuPic;

    /** 所属分类 ID。 */
    private Long catalogId;

    /** 品牌 ID。 */
    private Long brandId;

    /** 品牌名。 */
    private String brandName;

    /** 商品重量，单位千克。 */
    private BigDecimal weight;

    /** 上架状态：0 下架，1 上架。 */
    private Integer publishStatus;

    /** 创建时间。 */
    private Date createTime;

    /** 更新时间。 */
    private Date updateTime;

}
