package coupon.service;

import com.baomidou.mybatisplus.extension.service.IService;
import common.vo.PageVO;
import coupon.entity.SeckillPromotionEntity;

import java.util.Map;

import common.query.KeyPageQuery;
/**
 * 秒杀活动
 *
 * @author ??
 * @email sunlightcs@gmail.com
 * @date 2025-09-15 11:10:43
 */
public interface SeckillPromotionService extends IService<SeckillPromotionEntity> {

    PageVO<SeckillPromotionEntity> queryPage(KeyPageQuery query);
}

