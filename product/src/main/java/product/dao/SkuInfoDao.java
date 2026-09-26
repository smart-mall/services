package product.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import product.entity.SkuInfoEntity;

/**
 * {@code pms_sku_info} 表的 MyBatis-Plus Mapper，无自定义 SQL。
 */
@Mapper
public interface SkuInfoDao extends BaseMapper<SkuInfoEntity> {
	
}
