package product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
import product.vo.Catalog2Vo;

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

    @Cacheable(value = {"category"},key = "#root.method.name",sync = true)
    @Override
    public List<CategoryEntity> getLevel1Categories() {
        return this.baseMapper.selectList(
                new LambdaQueryWrapper<>( CategoryEntity.class).eq(CategoryEntity::getParentCid, 0));
    }

    @Cacheable(value = "category",key = "#root.method.name")
    @Override
    public Map<String, List<Catalog2Vo>> getCatalogJson() {

        //将数据库的多次查询变为一次
        List<CategoryEntity> selectList = this.baseMapper.selectList(null);

        //1、查出所有分类
        //1、1）查出所有一级分类
        List<CategoryEntity> level1Categorys = getParent_cid(selectList, 0L);

        //封装数据
        Map<String, List<Catalog2Vo>> parentCid = level1Categorys.stream().collect(Collectors.toMap(k -> k.getCatId().toString(), v -> {
            //1、每一个的一级分类,查到这个一级分类的二级分类
            List<CategoryEntity> categoryEntities = getParent_cid(selectList, v.getCatId());

            //2、封装上面的结果
            List<Catalog2Vo> catalog2Vos = null;
            if (categoryEntities != null) {
                catalog2Vos = categoryEntities.stream().map(l2 -> {
                    Catalog2Vo catalog2Vo = new Catalog2Vo(v.getCatId().toString(), null, l2.getCatId().toString(), l2.getName().toString());

                    //1、找当前二级分类的三级分类封装成vo
                    List<CategoryEntity> level3catalog = getParent_cid(selectList, l2.getCatId());

                    if (level3catalog != null) {
                        List<Catalog2Vo.Catalog3Vo> category3Vos = level3catalog.stream().map(l3 -> {
                            //2、封装成指定格式

                            return new Catalog2Vo.Catalog3Vo(l2.getCatId().toString(), l3.getCatId().toString(), l3.getName());
                        }).collect(Collectors.toList());
                        catalog2Vo.setCatalog3List(category3Vos);
                    }

                    return catalog2Vo;
                }).collect(Collectors.toList());
            }

            return catalog2Vos;
        }));

        return parentCid;
    }

    private List<CategoryEntity> getParent_cid(List<CategoryEntity> selectList,Long parentCid) {
        return selectList.stream().filter(item -> item.getParentCid().equals(parentCid)).collect(Collectors.toList());
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