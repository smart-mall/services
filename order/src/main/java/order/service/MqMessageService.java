package order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import common.utils.PageUtils;
import order.entity.MqMessageEntity;

import java.util.Map;

/**
 * 
 *
 * @author ??
 * @email sunlightcs@gmail.com
 * @date 2025-09-15 11:22:13
 */
public interface MqMessageService extends IService<MqMessageEntity> {

    PageUtils queryPage(Map<String, Object> params);
}

