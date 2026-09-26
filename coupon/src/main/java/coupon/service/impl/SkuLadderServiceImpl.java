package coupon.service.impl;

import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import common.vo.PageVO;

import coupon.dao.SkuLadderDao;
import coupon.entity.SkuLadderEntity;
import coupon.service.SkuLadderService;


import common.query.PageQuery;
/**
 * 商品阶梯价分页查询实现，不带筛选条件，直接按分页参数取单表数据。
 *
 * <p>无状态，线程安全；单表分页由 MyBatis-Plus 的 {@link ServiceImpl} 提供。
 */
@Service("skuLadderService")
public class SkuLadderServiceImpl extends ServiceImpl<SkuLadderDao, SkuLadderEntity> implements SkuLadderService {

    /** {@inheritDoc} */
    @Override
    public PageVO<SkuLadderEntity> queryPage(PageQuery query) {
        IPage<SkuLadderEntity> page = this.page(query.toPage());

        return new PageVO<>(page.getTotal(), page.getRecords());
    }

}