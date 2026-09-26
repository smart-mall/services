package product.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import product.entity.CategoryEntity;

/**
 * 商品三级分类
 */
@Mapper
public interface CategoryDao extends BaseMapper<CategoryEntity> {
	
}
