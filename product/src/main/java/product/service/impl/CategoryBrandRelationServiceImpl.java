package product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import common.vo.PageVO;
import org.springframework.stereotype.Service;
import product.dao.BrandDao;
import product.dao.CategoryBrandRelationDao;
import product.dao.CategoryDao;
import product.entity.BrandEntity;
import product.entity.CategoryBrandRelationEntity;
import product.service.CategoryBrandRelationService;

import java.util.ArrayList;
import java.util.List;


import common.query.PageQuery;
/**
 * 品牌分类关联服务的默认实现，基于 MyBatis-Plus 的 {@code ServiceImpl} 读写
 * {@code pms_category_brand_relation}。
 *
 * <p>关联行里冗余了品牌名与分类名：新增时回查两张主表填入，品牌或分类改名时由
 * {@code BrandServiceImpl} 与 {@code CategoryServiceImpl} 回写。
 */
@Service("categoryBrandRelationService")
public class CategoryBrandRelationServiceImpl extends ServiceImpl<CategoryBrandRelationDao, CategoryBrandRelationEntity> implements CategoryBrandRelationService {
    private final CategoryDao categoryDao;
    private final BrandDao brandDao;

    /**
     * 由容器注入分类与品牌 Mapper 构造。
     *
     * @param categoryDao 分类 Mapper，新增关联时回查分类名
     * @param brandDao 品牌 Mapper，新增关联时回查品牌名，按分类取品牌时批量查品牌
     */
    public CategoryBrandRelationServiceImpl(CategoryDao categoryDao, BrandDao brandDao) {
        this.categoryDao = categoryDao;
        this.brandDao = brandDao;
    }

    /** {@inheritDoc} */
    @Override
    public PageVO<CategoryBrandRelationEntity> queryPage(PageQuery query) {
        IPage<CategoryBrandRelationEntity> page = this.page(query.toPage());

        return new PageVO<>(page.getTotal(), page.getRecords());
    }

    /** {@inheritDoc} */
    @Override
    public List<CategoryBrandRelationEntity> listCategoryBrandRelation(Long brandId) {
        return this.list(new LambdaQueryWrapper<>(CategoryBrandRelationEntity.class).eq(CategoryBrandRelationEntity::getBrandId, brandId));
    }

    /** {@inheritDoc} */
    @Override
    public void saveDetail(CategoryBrandRelationEntity categoryBrandRelation) {
        Long brandId = categoryBrandRelation.getBrandId();
        Long catalogId = categoryBrandRelation.getCatalogId();

        String brandName = brandDao.selectById(brandId).getName();
        String catalogName = categoryDao.selectById(catalogId).getName();

        categoryBrandRelation.setBrandName(brandName);
        categoryBrandRelation.setCatalogName(catalogName);

        this.save(categoryBrandRelation);
    }

    /** {@inheritDoc} */
    @Override
    public void updateBrand(Long brandId, String name) {
        LambdaUpdateWrapper<CategoryBrandRelationEntity> set = new LambdaUpdateWrapper<CategoryBrandRelationEntity>()
                .eq(CategoryBrandRelationEntity::getBrandId, brandId)
                .set(CategoryBrandRelationEntity::getBrandName, name);
        this.update(set);
    }

    /** {@inheritDoc} */
    @Override
    public void updateCategory(Long catId, String name) {
        LambdaUpdateWrapper<CategoryBrandRelationEntity> set = new LambdaUpdateWrapper<CategoryBrandRelationEntity>()
                .eq(CategoryBrandRelationEntity::getCatalogId, catId)
                .set(CategoryBrandRelationEntity::getCatalogName, name);
        this.update(set);
    }

    /** {@inheritDoc} */
    @Override
    public void deleteByBrandIds(List<Long> brandIds) {
        if (brandIds == null || brandIds.isEmpty()) {
            return;
        }
        this.remove(new LambdaQueryWrapper<CategoryBrandRelationEntity>()
                .in(CategoryBrandRelationEntity::getBrandId, brandIds));
    }

    /** {@inheritDoc} */
    @Override
    public List<BrandEntity> getBrandByCatId(Long catId) {
        List<CategoryBrandRelationEntity> list = this.list(new LambdaQueryWrapper<>(CategoryBrandRelationEntity.class).eq(CategoryBrandRelationEntity::getCatalogId, catId));
        if (list == null || list.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> ids = list.stream().map(CategoryBrandRelationEntity::getBrandId).toList();
        return brandDao.selectByIds(ids);
    }

}