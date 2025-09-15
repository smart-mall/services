package ware.service;

import com.baomidou.mybatisplus.extension.service.IService;
import common.utils.PageUtils;
import ware.entity.WareOrderTaskDetailEntity;

import java.util.Map;

/**
 * 库存工作单
 *
 * @author ??
 * @email sunlightcs@gmail.com
 * @date 2025-09-15 11:20:17
 */
public interface WareOrderTaskDetailService extends IService<WareOrderTaskDetailEntity> {

    PageUtils queryPage(Map<String, Object> params);
}

