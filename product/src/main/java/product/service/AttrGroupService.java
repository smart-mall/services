package product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import common.vo.PageVO;
import product.entity.AttrGroupEntity;
import product.vo.AttrGroupRelationVO;
import product.vo.AttrGroupRespVO;
import product.vo.AttrGroupWithAttrsVO;
import product.vo.SpuItemAttrGroupVo;

import java.util.List;
import java.util.Map;

import common.query.KeyPageQuery;
import common.query.PageQuery;
/**
 * 属性分组服务：维护三级分类下的属性分组，以及分组与基本属性的绑定关系。
 *
 * <p>分组图标存在 MinIO，删除分组时同步删除图标对象；属性本身不随分组删除。
 */
public interface AttrGroupService extends IService<AttrGroupEntity> {

    /**
     * 分页查询全部属性分组，不带分类过滤。
     *
     * @param query 分页参数，{@code page} 为页码、{@code limit} 为每页条数，不能为 {@code null}
     * @return 分页结果，{@code rows} 为属性分组列表；没有数据时 {@code rows} 为空列表
     */
    PageVO<AttrGroupEntity> queryPage(PageQuery query);

    /**
     * 分页查询属性分组，并回填每个分组所属的分类名。
     *
     * @param query 分页与关键字条件，{@code key} 同时匹配分组名与分组 ID，不能为 {@code null}
     * @param categoryId 所属三级分类 ID，为 {@code null} 或 0 时不按分类过滤
     * @return 分页结果，每行含所属分类名；该分类下没有分组时 {@code rows} 为空列表
     */
    PageVO<AttrGroupRespVO> queryPage(KeyPageQuery query, Long categoryId);

    /**
     * 批量解除属性与属性分组的绑定。
     *
     * <p>只删关联行，属性本身不受影响；入参为 {@code null} 时直接返回，重复提交同一对
     * {@code attrId} 与 {@code attrGroupId} 不会报错。
     *
     * @param vos 关联入参数组，每项按 {@code attrId} 与 {@code attrGroupId} 定位一行，允许为 {@code null}
     */
    void deleteRelation(AttrGroupRelationVO[] vos);

    /**
     * 查询某三级分类下的全部属性分组，并带上每组已绑定的基本属性。
     *
     * @param catalogId 三级分类 ID，不能为 {@code null}
     * @return 分组列表，每组通过 {@code attrs} 携带已绑定的属性；该分类下没有分组时返回空列表
     */
    List<AttrGroupWithAttrsVO> getAttrGroupWithAttrs(Long catalogId);

    /**
     * 查询某 spu 的规格参数分组及每组下的属性取值，供商品详情页展示。
     *
     * @param spuId spu ID，不能为 {@code null}
     * @param catalogId 该 spu 所属三级分类 ID，不能为 {@code null}
     * @return 分组列表，每组含分组名与属性值；该 spu 没有规格参数时返回空列表
     */
    List<SpuItemAttrGroupVo> getAttrGroupWithAttrsBySpuId(Long spuId, Long catalogId);

    /**
     * 按主键批量删除属性分组。
     *
     * <p>分组与属性的关联行、分组的图标对象跟着一起删，属性本身不受影响；删除是幂等的，
     * 入参为 {@code null} 或查不到的 ID 会被静默跳过。
     *
     * @param list 待删除的分组主键列表，允许为 {@code null}
     * @throws common.exception.BaseException 分组图标对象删除失败时抛出，整批删除回滚
     */
    void deleteByIds(List<Long> list);

    /**
     * 修改属性分组，并在图标被替换时删掉被替换的图标对象。
     *
     * <p>先删被替换的图标再更新分组行：远程删除失败会抛业务异常并回滚修改，避免分组行改完而图标
     * 对象变成没人引用的孤儿。
     *
     * @param attrGroup 分组内容，{@code attrGroupId} 必填且对应分组必须已存在，不能为 {@code null}
     */
    void updateDetail(AttrGroupEntity attrGroup);
}

