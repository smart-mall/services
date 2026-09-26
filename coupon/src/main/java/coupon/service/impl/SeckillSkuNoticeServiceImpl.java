package coupon.service.impl;

import org.springframework.stereotype.Service;
import java.util.Map;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import common.vo.PageVO;

import coupon.dao.SeckillSkuNoticeDao;
import coupon.entity.SeckillSkuNoticeEntity;
import coupon.service.SeckillSkuNoticeService;


import common.query.PageQuery;
/**
 * 秒杀商品通知订阅的分页查询实现，不带筛选条件，直接按分页参数取单表数据。
 *
 * <p>无状态，线程安全；单表分页由 MyBatis-Plus 的 {@link ServiceImpl} 提供。
 */
@Service("seckillSkuNoticeService")
public class SeckillSkuNoticeServiceImpl extends ServiceImpl<SeckillSkuNoticeDao, SeckillSkuNoticeEntity> implements SeckillSkuNoticeService {

    /** {@inheritDoc} */
    @Override
    public PageVO<SeckillSkuNoticeEntity> queryPage(PageQuery query) {
        IPage<SeckillSkuNoticeEntity> page = this.page(query.toPage());

        return new PageVO<>(page.getTotal(), page.getRecords());
    }

}