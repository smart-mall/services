package common.mq.outbox;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 本地消息表的 Mapper。SQL 都用 Wrapper 表达，没有自定义语句。
 */
@Mapper
public interface OutboxDao extends BaseMapper<OutboxMessage> {
}
