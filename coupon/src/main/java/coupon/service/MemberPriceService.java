package coupon.service;

import com.baomidou.mybatisplus.extension.service.IService;
import common.vo.PageVO;
import coupon.entity.MemberPriceEntity;

import java.util.Map;

import common.query.KeyPageQuery;
/**
 * 商品会员价格
 */
public interface MemberPriceService extends IService<MemberPriceEntity> {

    PageVO<MemberPriceEntity> queryPage(KeyPageQuery query);
}

