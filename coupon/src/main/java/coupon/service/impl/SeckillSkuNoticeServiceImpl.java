package coupon.service.impl;

import org.springframework.stereotype.Service;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import common.vo.PageVO;

import coupon.dao.SeckillSkuNoticeDao;
import coupon.entity.SeckillSkuNoticeEntity;
import coupon.service.SeckillSkuNoticeService;


import common.query.PageQuery;
@Service("seckillSkuNoticeService")
public class SeckillSkuNoticeServiceImpl extends ServiceImpl<SeckillSkuNoticeDao, SeckillSkuNoticeEntity> implements SeckillSkuNoticeService {

    @Override
    public PageVO<SeckillSkuNoticeEntity> queryPage(PageQuery query) {
        IPage<SeckillSkuNoticeEntity> page = this.page(
                query.toPage(),
                new QueryWrapper<SeckillSkuNoticeEntity>()
        );

        return new PageVO<>(page.getTotal(), page.getRecords());
    }

}