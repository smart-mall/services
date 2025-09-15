package coupon.service;

import com.baomidou.mybatisplus.extension.service.IService;
import common.utils.PageUtils;
import coupon.entity.SeckillSessionEntity;

import java.util.Map;

/**
 * 秒杀活动场次
 *
 * @author ??
 * @email sunlightcs@gmail.com
 * @date 2025-09-15 11:10:43
 */
public interface SeckillSessionService extends IService<SeckillSessionEntity> {

    PageUtils queryPage(Map<String, Object> params);
}

