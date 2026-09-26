package coupon.service;

import com.baomidou.mybatisplus.extension.service.IService;
import common.vo.PageVO;
import coupon.entity.HomeSubjectSpuEntity;

import java.util.Map;

import common.query.PageQuery;
/**
 * 专题商品
 */
public interface HomeSubjectSpuService extends IService<HomeSubjectSpuEntity> {

    PageVO<HomeSubjectSpuEntity> queryPage(PageQuery query);
}

