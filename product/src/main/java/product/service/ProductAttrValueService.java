package product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import common.vo.PageVO;
import product.entity.ProductAttrValueEntity;

import java.util.List;
import java.util.Map;

import common.query.PageQuery;
/**
 * 商品规格参数值服务：维护每个 spu 的规格参数取值。
 *
 * <p>取值在上架时被快照进 Elasticsearch，因此已上架的商品不允许修改。
 */
public interface ProductAttrValueService extends IService<ProductAttrValueEntity> {

    /**
     * 分页查询全部规格参数值，不带筛选条件。
     *
     * @param query 分页参数，{@code page} 为页码、{@code limit} 为每页条数，不能为 {@code null}
     * @return 分页结果，{@code rows} 为规格参数值列表
     */
    PageVO<ProductAttrValueEntity> queryPage(PageQuery query);

    /**
     * 查询某 spu 的全部规格参数值。
     *
     * @param spuId spu ID，不能为 {@code null}
     * @return 该 spu 的规格参数值列表；未维护过规格参数时返回空列表
     */
    List<ProductAttrValueEntity> baseAttrListForSpu(Long spuId);

    /**
     * 全量覆盖某 spu 的规格参数值。
     *
     * <p>先按 {@code spuId} 删光原有取值再插入入参，入参里的 {@code spuId} 会被忽略并统一改写为
     * 方法入参的值；已上架的商品不允许修改，因为规格参数在上架时已快照进 Elasticsearch。
     *
     * @param spuId spu ID，不能为 {@code null}
     * @param entities 新的规格参数值列表，不能为 {@code null}；传空集合等同于清空该 spu 的规格参数
     * @throws common.exception.ValidationException spu 不存在时抛出
     * @throws common.exception.BaseException spu 已上架时抛出，修改整批不生效
     */
    void updateSpuAttr(Long spuId, List<ProductAttrValueEntity> entities);
}

