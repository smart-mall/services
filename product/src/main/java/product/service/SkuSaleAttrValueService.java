package product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import common.vo.PageVO;
import product.entity.SkuSaleAttrValueEntity;
import product.vo.SkuItemSaleAttrVo;

import java.util.List;
import java.util.Map;

import common.query.PageQuery;
/**
 * sku 销售属性值服务：维护每个 sku 的销售属性取值。
 */
public interface SkuSaleAttrValueService extends IService<SkuSaleAttrValueEntity> {

    /**
     * 分页查询全部 sku 销售属性值，不带筛选条件。
     *
     * @param query 分页参数，{@code page} 为页码、{@code limit} 为每页条数，不能为 {@code null}
     * @return 分页结果，{@code rows} 为销售属性值列表
     */
    PageVO<SkuSaleAttrValueEntity> queryPage(PageQuery query);

    /**
     * 查询某 spu 下全部 sku 的销售属性组合，供商品详情页渲染规格选择。
     *
     * @param spuId spu ID，不能为 {@code null}
     * @return 销售属性列表，每项含属性名、可选值以及拥有该值的 sku ID 集合；该 spu 没有销售属性时
     *         返回空列表
     */
    List<SkuItemSaleAttrVo> getSaleAttrBySpuId(Long spuId);

    /**
     * 查询某 sku 的全部销售属性取值。
     *
     * @param skuId sku ID，不能为 {@code null}
     * @return 形如 {@code 属性名：属性值} 的字符串列表；该 sku 没有销售属性时返回空列表
     */
    List<String> getSkuSaleAttrValuesAsStringList(Long skuId);
}

