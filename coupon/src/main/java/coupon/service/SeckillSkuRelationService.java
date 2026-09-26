package coupon.service;

import com.baomidou.mybatisplus.extension.service.IService;
import common.vo.PageVO;
import coupon.entity.SeckillSkuRelationEntity;

import java.util.Map;

import coupon.vo.SeckillSkuRelationPageQuery;
/**
 * 秒杀活动商品关联
 *
 * @author ??
 * @email sunlightcs@gmail.com
 * @date 2025-09-15 11:10:43
 */
public interface SeckillSkuRelationService extends IService<SeckillSkuRelationEntity> {

    PageVO<SeckillSkuRelationEntity> queryPage(SeckillSkuRelationPageQuery query);
}

