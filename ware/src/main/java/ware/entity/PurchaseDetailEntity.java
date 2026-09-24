package ware.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 
 * 
 * @author ??
 * @email sunlightcs@gmail.com
 * @date 2025-09-15 11:20:17
 */
@Data
@TableName("wms_purchase_detail")
public class PurchaseDetailEntity implements Serializable {
	@Serial private static final long serialVersionUID = 1L;

	/**
	 * 
	 */
	@TableId
	private Long id;
	/**
	 * 采购单id
	 */
	private Long purchaseId;
	/**
	 * 采购商品id
	 */
	private Long skuId;
	/**
	 * 采购数量
	 */
	private Integer skuNum;
	/**
	 * 采购金额
	 */
	private BigDecimal skuPrice;
	/**
	 * 仓库id
	 */
	private Long wareId;
	/**
	 * 状态[0新建，1已分配，2正在采购，3已完成，4采购失败]
	 */
	private Integer status;

	@TableField(exist = false)
	private String skuName =  "";

	@TableField(exist = false)
	private String wareName = "";

	/** 当前状态下允许的操作（edit/delete/merge/unassign），由列表接口填充，前端按钮据此显示 */
	@TableField(exist = false)
	private List<String> allowedActions;

}
