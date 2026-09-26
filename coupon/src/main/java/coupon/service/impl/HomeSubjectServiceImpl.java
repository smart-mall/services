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
@Service("homeSubjectService")
public class HomeSubjectServiceImpl extends ServiceImpl<HomeSubjectDao, HomeSubjectEntity> implements HomeSubjectService {

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