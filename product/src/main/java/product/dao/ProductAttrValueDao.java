package product.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import product.entity.ProductAttrValueEntity;

/**
 * {@code pms_product_attr_value} 表的 MyBatis-Plus Mapper，无自定义 SQL。
 */
@Mapper
public interface ProductAttrValueDao extends BaseMapper<ProductAttrValueEntity> {
	
}
