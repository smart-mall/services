package coupon.service;

import com.baomidou.mybatisplus.extension.service.IService;
import common.vo.PageVO;
import coupon.entity.HomeAdvEntity;

import java.util.Map;

import common.query.PageQuery;
/**
 * 首页轮播广告
 */
public interface HomeAdvService extends IService<HomeAdvEntity> {

    PageVO<HomeAdvEntity> queryPage(PageQuery query);
}

