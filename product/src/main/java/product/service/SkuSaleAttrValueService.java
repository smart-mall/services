package product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import common.vo.PageVO;
import product.entity.SkuSaleAttrValueEntity;
import product.vo.SkuItemSaleAttrVo;

import java.util.List;
import java.util.Map;

import common.query.PageQuery;
/**
 * sku销售属性&值
 */
public interface SkuSaleAttrValueService extends IService<SkuSaleAttrValueEntity> {

    PageVO<SkuSaleAttrValueEntity> queryPage(PageQuery query);

    List<SkuItemSaleAttrVo> getSaleAttrBySpuId(Long spuId);

    List<String> getSkuSaleAttrValuesAsStringList(Long skuId);
}

