package coupon.service;

import com.baomidou.mybatisplus.extension.service.IService;
import common.vo.PageVO;
import coupon.entity.SeckillSkuNoticeEntity;

import java.util.Map;

import common.query.PageQuery;
/**
 * 秒杀商品通知订阅
 *
 * @author ??
 * @email sunlightcs@gmail.com
 * @date 2025-09-15 11:10:43
 */
public interface SeckillSkuNoticeService extends IService<SeckillSkuNoticeEntity> {

    PageVO<SeckillSkuNoticeEntity> queryPage(PageQuery query);
}

