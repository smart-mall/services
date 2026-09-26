package coupon.service.impl;

import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import common.vo.PageVO;

import coupon.dao.HomeSubjectSpuDao;
import coupon.entity.HomeSubjectSpuEntity;
import coupon.service.HomeSubjectSpuService;


import common.query.PageQuery;
/**
 * 专题商品关联的分页查询实现，不带筛选条件，直接按分页参数取单表数据。
 *
 * <p>无状态，线程安全；单表分页由 MyBatis-Plus 的 {@link ServiceImpl} 提供。
 */
@Service("homeSubjectSpuService")
public class HomeSubjectSpuServiceImpl extends ServiceImpl<HomeSubjectSpuDao, HomeSubjectSpuEntity> implements HomeSubjectSpuService {

    /** {@inheritDoc} */
    @Override
    public PageVO<HomeSubjectSpuEntity> queryPage(PageQuery query) {
        IPage<HomeSubjectSpuEntity> page = this.page(query.toPage());

        return new PageVO<>(page.getTotal(), page.getRecords());
    }

}