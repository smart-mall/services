package ware.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serial;
import java.io.Serializable;
import lombok.Data;

/**
 * 仓库信息，对应 {@code wms_ware_info} 表，一行是一个可发货的实体仓库。
 *
 * <p>库存、采购需求、采购单与库存工作单都按仓库 ID 关联到它。
 */
@Data
@TableName("wms_ware_info")
public class WareInfoEntity implements Serializable {
	@Serial private static final long serialVersionUID = 1L;

	/** 主键 ID。 */
	@TableId
	private Long id;
	/** 仓库名。 */
	private String name;
	/** 仓库地址。 */
	private String address;
	/** 区域编码，取行政区划编码。 */
	private String areacode;

}
