package product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import common.vo.PageVO;
import product.entity.BrandEntity;
import product.entity.CategoryBrandRelationEntity;

import java.util.List;
import java.util.Map;

import common.query.PageQuery;
/**
 * 品牌分类关联
 *
 * @author ??
 * @email sunlightcs@gmail.com
 * @date 2025-09-15 09:58:33
 */
public interface CategoryBrandRelationService extends IService<CategoryBrandRelationEntity> {

    PageVO<CategoryBrandRelationEntity> queryPage(PageQuery query);

    List<CategoryBrandRelationEntity> listCategoryBrandRelation(Long brandId);

    void saveDetail(CategoryBrandRelationEntity categoryBrandRelation);

    void updateBrand(Long brandId, String name);

    void updateCategory(Long catId, String name);

    void deleteByBrandIds(List<Long> brandIds);

    List<BrandEntity> getBrandByCatId(Long catId);
}

