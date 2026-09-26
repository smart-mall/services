package product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import common.vo.PageVO;
import product.entity.CategoryEntity;
import product.vo.CategoryVo;

import java.util.List;

import common.query.PageQuery;
/**
 * 商品三级分类服务：维护按 {@code parent_cid} 自关联的分类树，以及分类被其它数据引用时的删除守卫。
 *
 * <p>分类树物理删除时连同整棵子树一起删，不做逻辑删除。
 */
public interface CategoryService extends IService<CategoryEntity> {

    /**
     * 分页查询全部三级分类，不组装父子层级关系。
     *
     * @param query 分页参数，{@code page} 为页码、{@code limit} 为每页条数，不能为 {@code null}
     * @return 分页结果，{@code rows} 为扁平的分类列表；没有数据时 {@code rows} 为空列表
     */
    PageVO<CategoryEntity> queryPage(PageQuery query);

    /**
     * 返回全部三级分类的树形结构，供后台菜单树使用。
     *
     * <p>不按 {@code showStatus} 过滤，隐藏的分类也会出现；一级分类与各级子分类均按 {@code sort} 升序。
     *
     * @return 一级分类列表，子分类通过 {@code children} 嵌套；没有分类时返回空列表
     */
    List<CategoryEntity> listWithTree();

    /**
     * 按主键批量删除分类，连同其全部子分类一起物理删除。
     *
     * <p>子树下还挂着品牌关联、商品、属性组或属性时整批拒绝，需要先处理；删除是幂等的，入参为
     * {@code null} 或查不到的 ID 会被静默跳过。删除成功后清空 {@code category} 缓存。
     *
     * @param list 待删除的分类主键列表，允许为 {@code null}
     * @throws common.exception.BaseException 子树下仍有引用数据时抛出，删除整批不生效
     */
    void removeMenuByIds(List<Long> list);

    /**
     * 返回从一级分类到指定分类的完整 ID 路径。
     *
     * @param catId 分类 ID，为 {@code null} 或分类不存在时返回空列表
     * @return 分类 ID 路径，顺序从一级分类到 {@code catId} 自身
     */
    List<Long> findcatalogIds(Long catId);

    /**
     * 修改分类，并同步刷新品牌分类关联表里冗余的分类名。
     *
     * @param category 分类内容，{@code catId} 必填，不能为 {@code null}
     */
    void updateDetail(CategoryEntity category);

    /**
     * 返回前台首页与全局导航使用的完整三级分类树。
     *
     * <p>只含 {@code showStatus} 为 1 的分类，一级分类与各级子分类均按 {@code sort} 升序；结果带
     * {@code category} 缓存，分类的增删改会整体清除它。
     *
     * @return 一级分类列表，每个节点通过 children 嵌套二级、三级分类
     */
    List<CategoryVo> getCatalogTree();
}

