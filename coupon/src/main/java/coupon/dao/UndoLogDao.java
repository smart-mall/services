package coupon.dao;

import coupon.entity.UndoLogEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UndoLogDao extends BaseMapper<UndoLogEntity> {
	
}
