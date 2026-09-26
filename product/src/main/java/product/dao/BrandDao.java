package product.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import product.entity.BrandEntity;

/**
 * 品牌
 */
@Mapper
public interface BrandDao extends BaseMapper<BrandEntity> {
	
}
