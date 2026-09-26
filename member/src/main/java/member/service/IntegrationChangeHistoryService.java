package member.service;

import com.baomidou.mybatisplus.extension.service.IService;
import common.vo.PageVO;
import member.entity.IntegrationChangeHistoryEntity;

import java.util.Map;

import common.query.PageQuery;
/**
 * 积分变化历史记录
 */
public interface IntegrationChangeHistoryService extends IService<IntegrationChangeHistoryEntity> {

    PageVO<IntegrationChangeHistoryEntity> queryPage(PageQuery query);
}

