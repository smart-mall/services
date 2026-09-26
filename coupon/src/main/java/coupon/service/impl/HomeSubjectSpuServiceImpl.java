package coupon.service.impl;

import org.springframework.stereotype.Service;
import java.util.Map;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import common.vo.PageVO;

import coupon.dao.HomeSubjectSpuDao;
import coupon.entity.HomeSubjectSpuEntity;
import coupon.service.HomeSubjectSpuService;


import common.query.PageQuery;
@Service("homeSubjectSpuService")
public class HomeSubjectSpuServiceImpl extends ServiceImpl<HomeSubjectSpuDao, HomeSubjectSpuEntity> implements HomeSubjectSpuService {

    @Override
    public PageVO<HomeSubjectSpuEntity> queryPage(PageQuery query) {
        IPage<HomeSubjectSpuEntity> page = this.page(query.toPage());

        return new PageVO<>(page.getTotal(), page.getRecords());
    }

}