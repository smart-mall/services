package coupon.service;

import com.baomidou.mybatisplus.extension.service.IService;
import common.vo.PageVO;
import coupon.entity.SeckillSessionEntity;

import java.util.List;
import java.util.Map;

import common.query.KeyPageQuery;
/**
 * 秒杀活动场次
 *
 * @author ??
 * @email sunlightcs@gmail.com
 * @date 2025-09-15 11:10:43
 */
public interface SeckillSessionService extends IService<SeckillSessionEntity> {

    PageVO<SeckillSessionEntity> queryPage(KeyPageQuery query);

    List<SeckillSessionEntity> getLates3DaySession();
}

