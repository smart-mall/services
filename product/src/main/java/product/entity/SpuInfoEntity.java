package product.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * SPU 信息，对应 {@code pms_spu_info} 表：商品的标准产品单元，其下按销售属性取值组合拆分为多个 SKU。
 *
 * <p>上下架状态由 {@code publishStatus} 表示，商品列表按它过滤出已上架的商品。
 */
@Data
@TableName("pms_spu_info")
public class SpuInfoEntity implements Serializable {
	@Serial private static final long serialVersionUID = 1L;

	/** 主键。 */
	@TableId
	private Long id;
	/** 商品名称。 */
	private String spuName;
	/** 商品描述。 */
	private String spuDescription;
	/**
	 * 所属分类 ID，指向 {@code pms_category.cat_id}。
	 */
	private Long catalogId;
	/**
	 * 品牌 ID，指向 {@code pms_brand.brand_id}。
	 */
	private Long brandId;
	/** 商品重量，单位：千克。 */
	private BigDecimal weight;
	/** 上架状态[0-新建，1-上架，2-下架]。 */
	private Integer publishStatus;
	/** 创建时间。 */
	private Date createTime;
	/** 更新时间。 */
	private Date updateTime;

	/** 品牌名；非数据库字段，列表查询时回填。 */
	@TableField(exist = false)
	private String brandName;

	/** 分类名；非数据库字段，列表查询时回填。 */
	@TableField(exist = false)
	private String catalogName;

	/** SPU 主图地址；非数据库字段，按 skuId 反查商品时从图集里取一张回填。 */
	@TableField(exist = false)
	private String spuPic;

}
