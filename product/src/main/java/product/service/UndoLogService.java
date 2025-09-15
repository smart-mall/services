package product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import common.utils.PageUtils;
import product.entity.UndoLogEntity;

import java.util.Map;

/**
 * 
 *
 * @author ??
 * @email sunlightcs@gmail.com
 * @date 2025-09-15 09:58:33
 */
public interface UndoLogService extends IService<UndoLogEntity> {

    PageUtils queryPage(Map<String, Object> params);
}

