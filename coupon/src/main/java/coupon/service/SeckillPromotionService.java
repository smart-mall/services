package coupon.service;

import com.baomidou.mybatisplus.extension.service.IService;
import common.vo.PageVO;
import coupon.entity.SeckillPromotionEntity;

import java.util.Map;

import common.query.KeyPageQuery;
/**
 * 秒杀活动
 */
public interface SeckillPromotionService extends IService<SeckillPromotionEntity> {

    PageVO<SeckillPromotionEntity> queryPage(KeyPageQuery query);
}

