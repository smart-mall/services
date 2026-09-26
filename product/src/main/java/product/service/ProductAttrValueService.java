package product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import common.vo.PageVO;
import product.entity.ProductAttrValueEntity;

import java.util.List;
import java.util.Map;

import common.query.PageQuery;
/**
 * spu属性值
 *
 * @author ??
 * @email sunlightcs@gmail.com
 * @date 2025-09-15 09:58:33
 */
public interface ProductAttrValueService extends IService<ProductAttrValueEntity> {

    PageVO<ProductAttrValueEntity> queryPage(PageQuery query);

    List<ProductAttrValueEntity> baseAttrListForSpu(Long spuId);

    void updateSpuAttr(Long spuId, List<ProductAttrValueEntity> entities);
}

