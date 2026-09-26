package member.service;

import com.baomidou.mybatisplus.extension.service.IService;
import common.vo.PageVO;
import member.entity.GrowthChangeHistoryEntity;

import java.util.Map;

import common.query.PageQuery;
/**
 * 成长值变化历史记录
 */
public interface GrowthChangeHistoryService extends IService<GrowthChangeHistoryEntity> {

    PageVO<GrowthChangeHistoryEntity> queryPage(PageQuery query);
}

