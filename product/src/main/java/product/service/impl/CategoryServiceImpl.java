package product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import common.utils.PageUtils;
import common.utils.Query;
import org.springframework.stereotype.Service;
import product.dao.CategoryDao;
import product.entity.CategoryEntity;
import product.service.CategoryService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Service("categoryService")
public class CategoryServiceImpl extends ServiceImpl<CategoryDao, CategoryEntity> implements CategoryService {

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<CategoryEntity> page = this.page(
                new Query<CategoryEntity>().getPage(params),
                new QueryWrapper<CategoryEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public List<CategoryEntity> listWithTree() {
        // 先获取所有分类
        List<CategoryEntity> categoryEntities = baseMapper.selectList(null);

        // 获取以及分类id映射表
        Map<Long, CategoryEntity> categoryEntityMap = categoryEntities.stream().collect(Collectors.toMap(CategoryEntity::getCatId, v -> v));

        // 一级菜单list
        List<CategoryEntity> level1Menus = categoryEntities.stream().filter(categoryEntity -> categoryEntity.getParentCid() == 0).toList();

        categoryEntities.forEach(categoryEntity -> {
            if (categoryEntity.getParentCid() == 0) {
                return;
            }
            // 先找到父菜单
            CategoryEntity categoryParent = categoryEntityMap.get(categoryEntity.getParentCid());
            if (categoryParent.getChildren() == null) {
                categoryParent.setChildren(new ArrayList<>());
            }
            categoryParent.getChildren().add(categoryEntity);
        });

        return level1Menus;
    }

    @Override
    public void removeMenuByIds(List<Long> list) {
        baseMapper.deleteByIds(list);
    }

}