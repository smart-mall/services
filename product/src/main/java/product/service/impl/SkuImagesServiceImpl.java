package product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import common.vo.PageVO;
import org.springframework.stereotype.Service;
import product.dao.SkuImagesDao;
import product.entity.SkuImagesEntity;
import product.service.SkuImagesService;

import java.util.List;
import java.util.Map;


import common.query.PageQuery;
@Service("skuImagesService")
public class SkuImagesServiceImpl extends ServiceImpl<SkuImagesDao, SkuImagesEntity> implements SkuImagesService {

    @Override
    public PageVO<SkuImagesEntity> queryPage(PageQuery query) {
        IPage<SkuImagesEntity> page = this.page(query.toPage());

        return new PageVO<>(page.getTotal(), page.getRecords());
    }

    /**
     * 查询某个 sku 的图集
     *
     * 表 pms_sku_images 里 default_img = 1 的那一条是主图，和图集第一张通常是同一个地址，
     * 前端做缩略图列表时要展示全部图片，不要只展示默认图。
     */
    @Override
    public List<SkuImagesEntity> getImagesBySkuId(Long skuId) {
        return this.list(new LambdaQueryWrapper<SkuImagesEntity>()
                .eq(SkuImagesEntity::getSkuId, skuId));
    }
}