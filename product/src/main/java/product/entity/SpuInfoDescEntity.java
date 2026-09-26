package product.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * SPU 图文介绍，对应 {@code pms_spu_info_desc} 表：与 SPU 一一对应，保存商品详情页的介绍内容。
 *
 * <p>主键即 SPU ID，不依赖数据库自增，插入时必须显式赋值。
 */
@Data
@TableName("pms_spu_info_desc")
public class SpuInfoDescEntity implements Serializable {
	@Serial private static final long serialVersionUID = 1L;

	/**
	 * 主键，与 {@code pms_spu_info.id} 相同，插入时需显式赋值。
	 */
	@TableId(type = IdType.INPUT)
	private Long spuId;
	/**
	 * 商品介绍，多张图片地址拼接成一个长字符串。
	 */
	private String description;

}
