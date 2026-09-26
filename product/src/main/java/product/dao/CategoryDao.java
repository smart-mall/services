package product.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import product.entity.CategoryEntity;

/**
 * {@code pms_category} 表的 MyBatis-Plus Mapper，无自定义 SQL。
 */
@Mapper
public interface CategoryDao extends BaseMapper<CategoryEntity> {
	
}
