package product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import common.vo.PageVO;
import product.entity.AttrEntity;
import product.vo.AttrRespVO;
import product.vo.AttrVO;

import java.util.List;
import java.util.Map;

import common.query.PageQuery;
import common.query.KeyPageQuery;
/**
 * 商品属性服务：维护基本属性与销售属性，以及基本属性与属性分组的绑定关系。
 *
 * <p>基本属性会额外写属性分组关联行，销售属性不写；属性图标存在 MinIO，删除属性时同步删除。
 */
public interface AttrService extends IService<AttrEntity> {

    /**
     * 分页查询全部商品属性，不带分类与类型过滤。
     *
     * @param query 分页参数，{@code page} 为页码、{@code limit} 为每页条数，不能为 {@code null}
     * @return 分页结果，{@code rows} 为属性列表；没有数据时 {@code rows} 为空列表
     */
    PageVO<AttrEntity> queryPage(PageQuery query);

    /**
     * 新增商品属性；基本属性额外写一条属性与属性分组的关联行。
     *
     * @param attr 属性内容，基本属性需带 {@code attrGroupId}，不能为 {@code null}
     */
    void saveAttr(AttrVO attr);

    /**
     * 按分类与属性类型分页查询属性，并补全所属分组名、分组 ID 与分类名。
     *
     * <p>{@code attrType} 只区分 {@code base} 与其余取值：传 {@code base} 查基本属性，传其他任何值
     * （含拼错的值）一律按销售属性查询，不会报错。
     *
     * @param query 分页与关键字条件，{@code key} 同时匹配属性 ID 与属性名，不能为 {@code null}
     * @param categoryId 三级分类 ID，为 {@code null} 或 0 时不按分类过滤
     * @param attrType 属性类型，{@code base} 为基本属性，其余（含 {@code null}）为销售属性
     * @return 分页结果，每行含分组名、分组 ID 与分类名
     */
    PageVO<AttrRespVO> queryBaseAttrPage(KeyPageQuery query, Long categoryId, String attrType);

    /**
     * 按主键查询属性详情。
     *
     * <p>属性不存在时不返回 {@code null}，实现会在取值处抛异常，调用方需先确认 ID 有效。
     *
     * @param attrId 属性 ID，不能为 {@code null}
     * @return 属性详情，含所属分组名与分组 ID、分类名，以及从一级分类到本级分类的路径
     */
    AttrRespVO getAttrInfo(Long attrId);

    /**
     * 修改商品属性；基本属性会顺带新增或更新属性与属性分组的关联。
     *
     * <p>图标被替换时先让 third-party 删除被替换的图标对象，删除失败则抛业务异常并回滚修改；
     * 销售属性不动关联行。
     *
     * @param attr 属性内容，{@code attrId} 必填，不能为 {@code null}
     * @throws common.exception.BaseException 被替换的图标对象删除失败时抛出，修改整批回滚
     */
    void updateAttr(AttrVO attr);

    /**
     * 查询某属性分组已绑定的全部属性。
     *
     * @param attrGroupId 属性分组 ID，不能为 {@code null}
     * @return 已绑定到该分组的属性列表；没有绑定时返回空列表
     */
    List<AttrEntity> getRelationAttr(Long attrGroupId);

    /**
     * 分页查询可以绑定到该分组、但尚未被绑定的基本属性。
     *
     * <p>排除范围是<b>同分类下所有分组</b>已绑定的属性，而不只是本分组：一个属性在同一个分类里
     * 只能属于一个分组。
     *
     * @param attrGroupId 属性分组 ID，用于定位其所属分类，不能为 {@code null}
     * @param query 分页与关键字条件，{@code key} 同时匹配属性名与属性 ID，不能为 {@code null}
     * @return 分页结果，{@code rows} 为可绑定的属性列表
     */
    PageVO<AttrEntity> getNoRelationAttr(Long attrGroupId, KeyPageQuery query);

    /**
     * 从入参属性中筛出可作为检索条件的那些，即 {@code search_type} 为 1 的属性。
     *
     * @param attrIds 待筛选的属性 ID 列表，不能为 {@code null} 或空集合，否则拼出的 SQL 不合法
     * @return 可作为检索条件的属性 ID 列表；一个都不满足时返回空列表
     */
    List<Long> selectSearchAttrs(List<Long> attrIds);

    /**
     * 按主键批量删除商品属性。
     *
     * <p>属性还被商品规格参数或 sku 销售属性引用时整批拒绝；属性与属性分组的关联行、属性图标
     * 对象跟着一起删。删除是幂等的，入参为 {@code null} 或查不到的 ID 会被静默跳过。
     *
     * @param list 待删除的属性主键列表，允许为 {@code null}
     * @throws common.exception.BaseException 属性仍被引用，或图标对象删除失败时抛出，删除整批不生效
     */
    void deleteByIds(List<Long> list);
}

