package product.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import product.entity.SkuImagesEntity;

/**
 * {@code pms_sku_images} 表的 MyBatis-Plus Mapper，无自定义 SQL。
 */
@Mapper
public interface SkuImagesDao extends BaseMapper<SkuImagesEntity> {
	
}
