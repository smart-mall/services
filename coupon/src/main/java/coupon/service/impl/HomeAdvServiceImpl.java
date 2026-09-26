package coupon.service.impl;

import org.springframework.stereotype.Service;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import common.vo.PageVO;

import coupon.dao.HomeAdvDao;
import coupon.entity.HomeAdvEntity;
import coupon.service.HomeAdvService;


import common.query.PageQuery;
@Service("homeAdvService")
public class HomeAdvServiceImpl extends ServiceImpl<HomeAdvDao, HomeAdvEntity> implements HomeAdvService {

    @Override
    public PageVO<HomeAdvEntity> queryPage(PageQuery query) {
        IPage<HomeAdvEntity> page = this.page(
                query.toPage(),
                new QueryWrapper<HomeAdvEntity>()
        );

        return new PageVO<>(page.getTotal(), page.getRecords());
    }

}