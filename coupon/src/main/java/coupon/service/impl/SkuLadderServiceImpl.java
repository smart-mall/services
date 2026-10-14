package coupon.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import common.exception.BaseCodeEnum;
import common.exception.BaseException;
import common.vo.PageVO;
import org.springframework.transaction.annotation.Transactional;

import coupon.dao.SkuLadderDao;
import coupon.entity.SkuLadderEntity;
import coupon.service.SkuLadderService;


import common.query.PageQuery;
/**
 * 商品阶梯价的分页查询与覆盖保存实现。
 *
 * <p>无状态，线程安全；单表读写由 MyBatis-Plus 的 {@link ServiceImpl} 提供。
 */
@Service("skuLadderService")
public class SkuLadderServiceImpl extends ServiceImpl<SkuLadderDao, SkuLadderEntity> implements SkuLadderService {

    /** {@inheritDoc} */
    @Override
    public PageVO<SkuLadderEntity> queryPage(PageQuery query) {
        IPage<SkuLadderEntity> page = this.page(query.toPage());

        return new PageVO<>(page.getTotal(), page.getRecords());
    }

    /**
     * {@inheritDoc}
     *
     * <p>删除与插入在同一个事务内：中途失败时旧配置不会被先删掉。
     */
    @Override
    @Transactional
    public void saveLadder(SkuLadderEntity skuLadder) {
        if (skuLadder == null || skuLadder.getSkuId() == null) {
            throw new BaseException(BaseCodeEnum.VALID_EXCEPTION, "阶梯价必须指定 SKU");
        }
        this.remove(new LambdaQueryWrapper<SkuLadderEntity>()
                .eq(SkuLadderEntity::getSkuId, skuLadder.getSkuId()));
        this.save(skuLadder);
    }

}