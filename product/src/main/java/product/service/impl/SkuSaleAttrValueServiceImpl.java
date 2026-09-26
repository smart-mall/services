package product.service.impl;

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
/**
 * sku 销售属性值服务的默认实现，基于 MyBatis-Plus 的 {@code ServiceImpl} 读写
 * {@code pms_sku_sale_attr_value}。
 *
 * <p>两个查询都走 {@code SkuSaleAttrValueDao} 的 XML SQL：一个按属性分组聚合出拥有该值的 sku 集合，
 * 一个把属性名与属性值拼成字符串。
 */
@Service("skuSaleAttrValueService")
public class SkuSaleAttrValueServiceImpl extends ServiceImpl<SkuSaleAttrValueDao, SkuSaleAttrValueEntity> implements SkuSaleAttrValueService {

    /** {@inheritDoc} */
    @Override
    public PageVO<SkuSaleAttrValueEntity> queryPage(PageQuery query) {
        IPage<SkuSaleAttrValueEntity> page = this.page(query.toPage());

        return new PageVO<>(page.getTotal(), page.getRecords());
    }

    /** {@inheritDoc} */
    @Override
    public List<SkuItemSaleAttrVo> getSaleAttrBySpuId(Long spuId) {

        SkuSaleAttrValueDao baseMapper = this.getBaseMapper();

        return baseMapper.getSaleAttrBySpuId(spuId);
    }

    /** {@inheritDoc} */
    @Override
    public List<String> getSkuSaleAttrValuesAsStringList(Long skuId) {

        SkuSaleAttrValueDao baseMapper = this.baseMapper;

        return baseMapper.getSkuSaleAttrValuesAsStringList(skuId);
    }

}