package ware.dao;

import ware.entity.PurchaseEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * {@code wms_purchase} 表的 MyBatis-Plus Mapper，映射采购单 {@link PurchaseEntity}。
 *
 * <p>未声明自定义 SQL，仅使用 {@link BaseMapper} 提供的通用增删改查。
 */
@Mapper
public interface PurchaseDao extends BaseMapper<PurchaseEntity> {
	
}
