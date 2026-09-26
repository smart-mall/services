package order.dao;

import order.entity.UndoLogEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * {@code undo_log} 表的 MyBatis-Plus Mapper，映射 Seata 分支事务的回滚日志 {@link UndoLogEntity}。
 *
 * <p>未声明自定义 SQL，仅使用 {@link BaseMapper} 提供的通用增删改查。
 */
@Mapper
public interface UndoLogDao extends BaseMapper<UndoLogEntity> {
	
}
