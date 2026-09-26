package ware.dao;

import ware.entity.PurchaseDetailEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * {@code wms_purchase_detail} 表的 MyBatis-Plus Mapper，映射采购需求单 {@link PurchaseDetailEntity}。
 *
 * <p>未声明自定义 SQL，仅使用 {@link BaseMapper} 提供的通用增删改查。
 */
@Mapper
public interface PurchaseDetailDao extends BaseMapper<PurchaseDetailEntity> {
	
}
