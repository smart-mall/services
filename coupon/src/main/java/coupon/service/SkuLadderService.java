package coupon.service;

import com.baomidou.mybatisplus.extension.service.IService;
import common.vo.PageVO;
import coupon.entity.SkuLadderEntity;

import java.util.Map;

import common.query.PageQuery;
/**
 * 商品阶梯价格
 */
public interface SkuLadderService extends IService<SkuLadderEntity> {

    PageVO<SkuLadderEntity> queryPage(PageQuery query);
}

