package product.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import product.entity.AttrEntity;

/**
 * 商品属性
 * 
 * @author ??
 * @email sunlightcs@gmail.com
 * @date 2025-09-15 09:58:33
 */
@Mapper
public interface AttrDao extends BaseMapper<AttrEntity> {
	
}
