package ware.service;

import com.baomidou.mybatisplus.extension.service.IService;
import common.utils.PageUtils;
import ware.entity.WareOrderTaskEntity;

import java.util.Map;

/**
 * 库存工作单
 *
 * @author ??
 * @email sunlightcs@gmail.com
 * @date 2025-09-15 11:20:17
 */
public interface WareOrderTaskService extends IService<WareOrderTaskEntity> {

    PageUtils queryPage(Map<String, Object> params);
}

