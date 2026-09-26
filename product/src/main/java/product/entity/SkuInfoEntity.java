package product.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * SKU 信息，对应 {@code pms_sku_info} 表：SPU 按销售属性取值组合拆出的可售单元，售价与图集都记录在 SKU 维度。
 *
 * <p>销售属性取值见 {@link SkuSaleAttrValueEntity}，图集见 {@link SkuImagesEntity}。
 */
@Data
@TableName("pms_sku_info")
public class SkuInfoEntity implements Serializable {
	@Serial private static final long serialVersionUID = 1L;

	/** 主键。 */
	@TableId
	private Long skuId;
	/**
	 * 所属 SPU ID，指向 {@code pms_spu_info.id}。
	 */
	private Long spuId;
	/** SKU 名称，由商品名与销售属性取值拼接而成。 */
	private String skuName;
	/** SKU 介绍描述。 */
	private String skuDesc;
	/**
	 * 所属分类 ID，指向 {@code pms_category.cat_id}。
	 */
	private Long catalogId;
	/**
	 * 品牌 ID，指向 {@code pms_brand.brand_id}。
	 */
	private Long brandId;
	/** 默认图片地址，列表与详情页的主图取此字段。 */
	private String skuDefaultImg;
	/** 标题。 */
	private String skuTitle;
	/** 副标题，用于促销文案。 */
	private String skuSubtitle;
	/** 售价，单位：元。 */
	private BigDecimal price;
	/** 销量。 */
	private Long saleCount;

}
