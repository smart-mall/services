package product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import common.vo.PageVO;
import org.springframework.stereotype.Service;
import product.dao.SkuImagesDao;
import product.entity.SkuImagesEntity;
import product.service.SkuImagesService;

import java.util.List;


import common.query.PageQuery;
/**
 * sku 图片服务的默认实现，基于 MyBatis-Plus 的 {@code ServiceImpl} 读写 {@code pms_sku_images}。
 *
 * <p>新增商品时会连带写入 sku 图集；本类实现分页查询与按 sku 取图集，增删改由继承的
 * {@code IService} 提供。
 */
@Service("skuImagesService")
public class SkuImagesServiceImpl extends ServiceImpl<SkuImagesDao, SkuImagesEntity> implements SkuImagesService {

    /** {@inheritDoc} */
    @Override
    public PageVO<SkuImagesEntity> queryPage(PageQuery query) {
        IPage<SkuImagesEntity> page = this.page(query.toPage());

        return new PageVO<>(page.getTotal(), page.getRecords());
    }

    /** {@inheritDoc} */
    @Override
    public List<SkuImagesEntity> getImagesBySkuId(Long skuId) {
        return this.list(new LambdaQueryWrapper<SkuImagesEntity>()
                .eq(SkuImagesEntity::getSkuId, skuId));
    }
}