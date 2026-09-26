package coupon.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import common.vo.PageVO;
import coupon.dao.HomeSubjectDao;
import coupon.entity.HomeSubjectEntity;
import coupon.service.HomeSubjectService;
import org.springframework.stereotype.Service;

import java.util.Map;


import common.query.KeyPageQuery;
/**
 * 首页专题分页查询实现，{@code key} 非空时同时模糊匹配专题名与专题 ID。
 *
 * <p>无状态，线程安全；单表分页由 MyBatis-Plus 的 {@link ServiceImpl} 提供。
 */
@Service("homeSubjectService")
public class HomeSubjectServiceImpl extends ServiceImpl<HomeSubjectDao, HomeSubjectEntity> implements HomeSubjectService {

    /** {@inheritDoc} */
    @Override
    public PageVO<HomeSubjectEntity> queryPage(KeyPageQuery query) {
        String key = query.getKey();
        LambdaQueryWrapper<HomeSubjectEntity> wrapper = new LambdaQueryWrapper<>();

        if (key != null && !key.isEmpty()) {
            wrapper.like(HomeSubjectEntity::getName, key)
                    .or()
                    .like(HomeSubjectEntity::getId, key);
        }
        IPage<HomeSubjectEntity> page = this.page(
                query.toPage(),
                wrapper
        );

        return new PageVO<>(page.getTotal(), page.getRecords());
    }

}