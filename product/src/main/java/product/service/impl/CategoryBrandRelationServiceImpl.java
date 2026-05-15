package product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import common.utils.PageUtils;
import common.utils.Query;
import org.springframework.stereotype.Service;
import product.dao.BrandDao;
import product.dao.CategoryBrandRelationDao;
import product.dao.CategoryDao;
import product.entity.BrandEntity;
import product.entity.CategoryBrandRelationEntity;
import product.service.CategoryBrandRelationService;

import java.util.List;
import java.util.Map;


@Service("categoryBrandRelationService")
public class CategoryBrandRelationServiceImpl extends ServiceImpl<CategoryBrandRelationDao, CategoryBrandRelationEntity> implements CategoryBrandRelationService {
    private final CategoryDao categoryDao;
    private final BrandDao brandDao;

    public CategoryBrandRelationServiceImpl(CategoryDao categoryDao, BrandDao brandDao) {
        this.categoryDao = categoryDao;
        this.brandDao = brandDao;
    }

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<CategoryBrandRelationEntity> page = this.page(
                new Query<CategoryBrandRelationEntity>().getPage(params),
                new QueryWrapper<>()
        );

        return new PageUtils(page);
    }

    @Override
    public List<CategoryBrandRelationEntity> listCategoryBrandRelation(Long brandId) {
        return this.list(new LambdaQueryWrapper<>(CategoryBrandRelationEntity.class).eq(CategoryBrandRelationEntity::getBrandId, brandId));
    }

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

    @Override
    public void updateBrand(Long brandId, String name) {
        LambdaUpdateWrapper<CategoryBrandRelationEntity> set = new LambdaUpdateWrapper<CategoryBrandRelationEntity>()
                .eq(CategoryBrandRelationEntity::getBrandId, brandId)
                .set(CategoryBrandRelationEntity::getBrandName, name);
        this.update(set);
    }

    @Override
    public void updateCategory(Long catId, String name) {
        LambdaUpdateWrapper<CategoryBrandRelationEntity> set = new LambdaUpdateWrapper<CategoryBrandRelationEntity>()
                .eq(CategoryBrandRelationEntity::getCatalogId, catId)
                .set(CategoryBrandRelationEntity::getCatalogName, name);
        this.update(set);
    }

    @Override
    public List<BrandEntity> getBrandByCatId(Long catId) {
        List<CategoryBrandRelationEntity> list = this.list(new LambdaQueryWrapper<>(CategoryBrandRelationEntity.class).eq(CategoryBrandRelationEntity::getCatalogId, catId));
        List<Long> ids = list.stream().map(CategoryBrandRelationEntity::getBrandId).toList();
        return brandDao.selectByIds(ids);
    }

}