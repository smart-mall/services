package product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import common.vo.PageVO;
import product.entity.BrandEntity;
import product.entity.CategoryBrandRelationEntity;

import java.util.List;
import java.util.Map;

import common.query.PageQuery;
/**
 * 品牌与分类关联服务：维护「哪些品牌挂在哪个三级分类下」。
 *
 * <p>关联行里冗余了品牌名与分类名，品牌或分类改名时由各自的服务回写。
 */
public interface CategoryBrandRelationService extends IService<CategoryBrandRelationEntity> {

    /**
     * 分页查询全部品牌分类关联行，不带筛选条件。
     *
     * @param query 分页参数，{@code page} 为页码、{@code limit} 为每页条数，不能为 {@code null}
     * @return 分页结果，{@code rows} 为关联行列表
     */
    PageVO<CategoryBrandRelationEntity> queryPage(PageQuery query);

    /**
     * 查询某品牌关联的全部分类。
     *
     * @param brandId 品牌 ID，不能为 {@code null}
     * @return 该品牌的分类关联行，含冗余的分类名；没有关联时返回空列表
     */
    List<CategoryBrandRelationEntity> listCategoryBrandRelation(Long brandId);

    /**
     * 新增品牌与分类的关联，冗余的品牌名与分类名由服务端回查填入。
     *
     * @param categoryBrandRelation 关联内容，{@code brandId} 与 {@code catalogId} 必填且对应的品牌、
     *                              分类必须已存在，不能为 {@code null}
     */
    void saveDetail(CategoryBrandRelationEntity categoryBrandRelation);

    /**
     * 把某品牌在关联表里的品牌名刷新为新值，供品牌改名时调用。
     *
     * <p>品牌没有关联行时不报错，只是没有行被更新。
     *
     * @param brandId 品牌 ID，不能为 {@code null}
     * @param name 新的品牌名，不能为 {@code null}
     */
    void updateBrand(Long brandId, String name);

    /**
     * 把某分类在关联表里的分类名刷新为新值，供分类改名时调用。
     *
     * <p>分类没有关联行时不报错，只是没有行被更新。
     *
     * @param catId 分类 ID，不能为 {@code null}
     * @param name 新的分类名，不能为 {@code null}
     */
    void updateCategory(Long catId, String name);

    /**
     * 删除这些品牌的全部分类关联行。
     *
     * <p>入参为 {@code null} 或空集合时直接返回；删除是幂等的。
     *
     * @param brandIds 品牌 ID 列表，允许为 {@code null}
     */
    void deleteByBrandIds(List<Long> brandIds);

    /**
     * 查询某分类下关联的全部品牌。
     *
     * @param catId 三级分类 ID，不能为 {@code null}
     * @return 品牌列表；没有关联或关联的品牌都已不存在时返回空列表
     */
    List<BrandEntity> getBrandByCatId(Long catId);
}

