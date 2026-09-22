package product.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import product.entity.MqMessageEntity;

/**
 * 本地消息表（outbox）。没有自定义 SQL，条件更新都在 service 里用 Wrapper 表达。
 */
@Mapper
public interface MqMessageDao extends BaseMapper<MqMessageEntity> {
}
