package product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import common.utils.PageUtils;
import common.utils.Query;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import product.dao.CategoryDao;
import product.entity.CategoryEntity;
import product.service.CategoryBrandRelationService;
import product.service.CategoryService;
import product.vo.CategoryVo;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
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

        // 获取一级分类id映射表
        Map<Long, CategoryEntity> categoryEntityMap = categoryEntities.stream()
                .collect(Collectors.toMap(CategoryEntity::getCatId, v -> v));

        // 一级菜单list（按sort升序排序）
        List<CategoryEntity> level1Menus = categoryEntities.stream()
                .filter(categoryEntity -> categoryEntity.getParentCid() == 0)
                .sorted(Comparator.comparingInt(CategoryEntity::getSort))
                .collect(Collectors.toList());

        // 构建父子关系
        categoryEntities.forEach(categoryEntity -> {
            if (categoryEntity.getParentCid() == 0) {
                return;
            }
            // 先找到父菜单
            CategoryEntity categoryParent = categoryEntityMap.get(categoryEntity.getParentCid());
            if (categoryParent != null) {
                if (categoryParent.getChildren() == null) {
                    categoryParent.setChildren(new ArrayList<>());
                }
                categoryParent.getChildren().add(categoryEntity);
            }
        });

        // 对所有层级的子菜单进行排序
        level1Menus.forEach(this::sortChildren);

        return level1Menus;
    }

    /**
     * 递归排序子节点
     */
    private void sortChildren(CategoryEntity category) {
        if (category.getChildren() != null && !category.getChildren().isEmpty()) {
            // 按 sort 升序排序
            category.getChildren().sort(Comparator.comparingInt(CategoryEntity::getSort));
            // 递归排序子节点的子节点
            category.getChildren().forEach(this::sortChildren);
        }
    }

    @Override
    public void removeMenuByIds(List<Long> list) {
        baseMapper.deleteByIds(list);
    }

    @Override
    public List<Long> findcatalogIds(Long catId) {
        // 初始化可变集合（LinkedList支持addFirst操作，效率高）
        List<Long> path = new LinkedList<>();
        findParentPath(catId, path);
        return path;
    }

    @Override
    @Transactional
    public void updateDetail(CategoryEntity category) {
        log.debug("先修改分类表");
        this.updateById(category);
        log.debug("修改品牌分类关联表");
        categoryBrandRelationService.updateCategory(category.getCatId(), category.getName());
    }

    /**
     * 前台首页/全局导航使用的完整三级分类树。
     *
     * 一次 selectList(null) 把分类全捞出来（show_status 由 CategoryEntity 上的 @TableLogic 自动过滤），
     * 后续组树全部在内存里完成，不再按层级反复查库，也不会出现 N+1。
     */
    @Cacheable(value = "category", key = "#root.method.name", sync = true)
    @Override
    public List<CategoryVo> getCatalogTree() {
        List<CategoryEntity> selectList = this.baseMapper.selectList(null);

        // 按 parentCid 分组，一次遍历代替每层都 stream().filter() 扫一遍全表
        Map<Long, List<CategoryEntity>> childrenByParentCid = selectList.stream()
                .collect(Collectors.groupingBy(CategoryEntity::getParentCid));

        return buildTree(childrenByParentCid, 0L);
    }

    /**
     * 递归把分类实体组装成 CategoryVo 树，每一层都按 sort 升序排序
     *
     * @param childrenByParentCid 已经按 parentCid 分好组的全部分类
     * @param parentCid           当前要组装的父分类id，一级分类传 0
     */
    private List<CategoryVo> buildTree(Map<Long, List<CategoryEntity>> childrenByParentCid, Long parentCid) {
        return childrenByParentCid.getOrDefault(parentCid, List.of()).stream()
                .sorted(Comparator.comparingInt(CategoryEntity::getSort))
                .map(category -> new CategoryVo(
                        category.getCatId(),
                        category.getName(),
                        category.getIcon(),
                        buildTree(childrenByParentCid, category.getCatId())))
                .collect(Collectors.toList());
    }



    /**
     * 递归填充父路径（使用可变集合作为参数传递，避免创建不可变集合）
     * @param catalogId 当前分类ID
     * @param path 用于存储完整路径的可变集合
     */
    private void findParentPath(Long catalogId, List<Long> path) {
        // 1. 查询当前分类信息（确保catalogId有效，避免空指针）
        if (catalogId == null) {
            return;
        }
        CategoryEntity categoryEntity = baseMapper.selectById(catalogId);
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