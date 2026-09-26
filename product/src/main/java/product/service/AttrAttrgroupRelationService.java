package product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import common.vo.PageVO;
import product.entity.AttrAttrgroupRelationEntity;
import product.vo.AttrGroupRelationVO;

import java.util.List;

import common.query.PageQuery;
/**
 * 属性与属性分组的关联服务：维护基本属性归属哪个属性分组。
 *
 * <p>关联行只表达归属关系，属性值不在这里维护。
 */
public interface AttrAttrgroupRelationService extends IService<AttrAttrgroupRelationEntity> {

    /**
     * 分页查询全部属性分组关联行，不带筛选条件。
     *
     * @param query 分页参数，{@code page} 为页码、{@code limit} 为每页条数，不能为 {@code null}
     * @return 分页结果，{@code rows} 为关联行列表
     */
    PageVO<AttrAttrgroupRelationEntity> queryPage(PageQuery query);

    /**
     * 批量把属性绑定到属性分组。
     *
     * <p>不校验重复：同一对 {@code attrId} 与 {@code attrGroupId} 重复提交会插入多行关联。
     *
     * @param vos 关联入参列表，每项需带 {@code attrId} 与 {@code attrGroupId}，不能为 {@code null}
     */
    void addRelation(List<AttrGroupRelationVO> vos);
}

