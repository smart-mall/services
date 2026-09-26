package coupon.service;

import com.baomidou.mybatisplus.extension.service.IService;
import common.vo.PageVO;
import coupon.entity.HomeSubjectSpuEntity;

import java.util.Map;

import common.query.PageQuery;
/**
 * 专题商品
 *
 * @author ??
 * @email sunlightcs@gmail.com
 * @date 2025-09-15 11:10:43
 */
public interface HomeSubjectSpuService extends IService<HomeSubjectSpuEntity> {

    PageVO<HomeSubjectSpuEntity> queryPage(PageQuery query);
}

