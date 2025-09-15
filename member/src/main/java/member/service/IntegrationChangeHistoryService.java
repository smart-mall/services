package member.service;

import com.baomidou.mybatisplus.extension.service.IService;
import common.utils.PageUtils;
import member.entity.IntegrationChangeHistoryEntity;

import java.util.Map;

/**
 * 积分变化历史记录
 *
 * @author ??
 * @email sunlightcs@gmail.com
 * @date 2025-09-15 11:14:17
 */
public interface IntegrationChangeHistoryService extends IService<IntegrationChangeHistoryEntity> {

    PageUtils queryPage(Map<String, Object> params);
}

