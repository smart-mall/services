package member.service;

import com.baomidou.mybatisplus.extension.service.IService;
import common.utils.PageUtils;
import member.entity.GrowthChangeHistoryEntity;

import java.util.Map;

/**
 * 成长值变化历史记录
 *
 * @author ??
 * @email sunlightcs@gmail.com
 * @date 2025-09-15 11:14:17
 */
public interface GrowthChangeHistoryService extends IService<GrowthChangeHistoryEntity> {

    PageUtils queryPage(Map<String, Object> params);
}

