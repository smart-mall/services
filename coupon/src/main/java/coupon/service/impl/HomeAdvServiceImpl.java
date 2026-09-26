package coupon.service.impl;

import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import common.vo.PageVO;

import coupon.dao.HomeAdvDao;
import coupon.entity.HomeAdvEntity;
import coupon.service.HomeAdvService;


import common.query.PageQuery;
/**
 * 首页轮播广告分页查询实现，不带筛选条件，直接按分页参数取单表数据。
 *
 * <p>无状态，线程安全；单表分页由 MyBatis-Plus 的 {@link ServiceImpl} 提供。
 */
@Service("homeAdvService")
public class HomeAdvServiceImpl extends ServiceImpl<HomeAdvDao, HomeAdvEntity> implements HomeAdvService {

    /** {@inheritDoc} */
    @Override
    public PageVO<HomeAdvEntity> queryPage(PageQuery query) {
        IPage<HomeAdvEntity> page = this.page(query.toPage());

        return new PageVO<>(page.getTotal(), page.getRecords());
    }

}