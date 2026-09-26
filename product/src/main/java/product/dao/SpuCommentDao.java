package product.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import product.entity.SpuCommentEntity;

/**
 * {@code pms_spu_comment} 表的 MyBatis-Plus Mapper，无自定义 SQL。
 */
@Mapper
public interface SpuCommentDao extends BaseMapper<SpuCommentEntity> {
	
}
