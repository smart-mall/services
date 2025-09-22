package product.service.impl;

import ch.qos.logback.core.util.StringUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import common.utils.PageUtils;
import common.utils.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import product.dao.CategoryDao;
import product.entity.CategoryEntity;
import product.service.CategoryBrandRelationService;
import product.service.CategoryService;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Service("categoryService")
public class CategoryServiceImpl extends ServiceImpl<CategoryDao, CategoryEntity> implements CategoryService {
    private final CategoryBrandRelationService categoryBrandRelationService;

    public CategoryServiceImpl(CategoryBrandRelationService categoryBrandRelationService) {
        this.categoryBrandRelationService = categoryBrandRelationService;
    }

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<CategoryEntity> page = this.page(
                new Query<CategoryEntity>().getPage(params),
                new QueryWrapper<>()
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

    @Override
    public List<Long> findCatelogIds(Long catId) {
        // 初始化可变集合（LinkedList支持addFirst操作，效率高）
        List<Long> path = new LinkedList<>();
        findParentPath(catId, path);
        return path;
    }

    @Override
    @Transactional
    public void updateDetail(CategoryEntity category) {
        this.save( category);
        if (StringUtil.isNullOrEmpty(category.getName())) {
            categoryBrandRelationService.updateCategory(category.getCatId(), category.getName());
        }
    }

    /**
     * 递归填充父路径（使用可变集合作为参数传递，避免创建不可变集合）
     * @param catelogId 当前分类ID
     * @param path 用于存储完整路径的可变集合
     */
    private void findParentPath(Long catelogId, List<Long> path) {
        // 1. 查询当前分类信息（确保catelogId有效，避免空指针）
        if (catelogId == null) {
            return;
        }
        CategoryEntity categoryEntity = baseMapper.selectById(catelogId);
        if (categoryEntity == null) {
            return;
        }

        // 2. 递归查找父分类（先找父级，再添加当前级，保证路径从顶级到当前级）
        if (categoryEntity.getParentCid() != 0) {
            findParentPath(categoryEntity.getParentCid(), path);
        }

        // 3. 将当前分类ID添加到路径中（此时父级已全部添加完成）
        path.add(categoryEntity.getCatId());
    }
}