package coupon.service;

import com.baomidou.mybatisplus.extension.service.IService;
import common.vo.PageVO;
import coupon.entity.HomeSubjectEntity;

import java.util.Map;

import common.query.KeyPageQuery;
/**
 * 首页专题表【jd首页下面很多专题，每个专题链接新的页面，展示专题商品信息】
 *
 * @author ??
 * @email sunlightcs@gmail.com
 * @date 2025-09-15 11:10:43
 */
public interface HomeSubjectService extends IService<HomeSubjectEntity> {

    PageVO<HomeSubjectEntity> queryPage(KeyPageQuery query);
}

