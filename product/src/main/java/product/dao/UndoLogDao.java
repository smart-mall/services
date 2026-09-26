package product.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import product.entity.UndoLogEntity;

@Mapper
public interface UndoLogDao extends BaseMapper<UndoLogEntity> {
	
}
