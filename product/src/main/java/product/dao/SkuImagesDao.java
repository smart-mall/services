package product.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import product.entity.SkuImagesEntity;

/**
 * sku图片
 */
@Mapper
public interface SkuImagesDao extends BaseMapper<SkuImagesEntity> {
	
}
