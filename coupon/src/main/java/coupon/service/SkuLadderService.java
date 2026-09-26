package coupon.service;

import com.baomidou.mybatisplus.extension.service.IService;
import common.vo.PageVO;
import coupon.entity.SkuLadderEntity;

import java.util.Map;

import common.query.PageQuery;
/**
 * 商品阶梯价格
 *
 * @author ??
 * @email sunlightcs@gmail.com
 * @date 2025-09-15 11:10:43
 */
public interface SkuLadderService extends IService<SkuLadderEntity> {

    PageVO<SkuLadderEntity> queryPage(PageQuery query);
}

