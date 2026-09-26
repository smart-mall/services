package product.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import product.entity.BrandEntity;

/**
 * {@code pms_brand} 表的 MyBatis-Plus Mapper，无自定义 SQL。
 */
@Mapper
public interface BrandDao extends BaseMapper<BrandEntity> {
	
}
