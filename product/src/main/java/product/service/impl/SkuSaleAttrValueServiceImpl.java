package product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import common.vo.PageVO;
import org.springframework.stereotype.Service;
import product.dao.SkuSaleAttrValueDao;
import product.entity.SkuSaleAttrValueEntity;
import product.service.SkuSaleAttrValueService;
import product.vo.SkuItemSaleAttrVo;

import java.util.List;
import java.util.Map;


import common.query.PageQuery;
@Service("skuSaleAttrValueService")
public class SkuSaleAttrValueServiceImpl extends ServiceImpl<SkuSaleAttrValueDao, SkuSaleAttrValueEntity> implements SkuSaleAttrValueService {

    @Override
    public PageVO<SkuSaleAttrValueEntity> queryPage(PageQuery query) {
        IPage<SkuSaleAttrValueEntity> page = this.page(
                query.toPage(),
                new QueryWrapper<SkuSaleAttrValueEntity>()
        );

        return new PageVO<>(page.getTotal(), page.getRecords());
    }

    @Override
    public List<SkuItemSaleAttrVo> getSaleAttrBySpuId(Long spuId) {

        SkuSaleAttrValueDao baseMapper = this.getBaseMapper();

        return baseMapper.getSaleAttrBySpuId(spuId);
    }

    @Override
    public List<String> getSkuSaleAttrValuesAsStringList(Long skuId) {

        SkuSaleAttrValueDao baseMapper = this.baseMapper;

        return baseMapper.getSkuSaleAttrValuesAsStringList(skuId);
    }

}