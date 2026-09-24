package product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import common.exception.BaseCodeEnum;
import common.exception.BaseException;
import common.utils.PageUtils;
import common.utils.Query;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import product.dao.AttrDao;
import product.dao.AttrGroupDao;
import product.dao.CategoryDao;
import product.dao.SpuInfoDao;
import product.entity.AttrEntity;
import product.entity.AttrGroupEntity;
import product.entity.CategoryBrandRelationEntity;
import product.entity.CategoryEntity;
import product.entity.SpuInfoEntity;
import product.service.CategoryBrandRelationService;
import product.service.CategoryService;
import product.vo.CategoryVo;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service("categoryService")
public class CategoryServiceImpl extends ServiceImpl<CategoryDao, CategoryEntity> implements CategoryService {
    private final CategoryBrandRelationService categoryBrandRelationService;

    // 注入 DAO 而不是对应的 Service：那三个 Service 都依赖 CategoryService，会构造器循环
    private final SpuInfoDao spuInfoDao;
    private final AttrDao attrDao;
    private final AttrGroupDao attrGroupDao;

    public CategoryServiceImpl(CategoryBrandRelationService categoryBrandRelationService,
                               SpuInfoDao spuInfoDao,
                               AttrDao attrDao,
                               AttrGroupDao attrGroupDao) {
        this.categoryBrandRelationService = categoryBrandRelationService;
        this.spuInfoDao = spuInfoDao;
        this.attrDao = attrDao;
        this.attrGroupDao = attrGroupDao;
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

    /**
     * 删除分类，连同子分类一起物理删除；子树下还挂着品牌关联、商品、属性组或属性时整批拒绝。
     */
    @Override
    @Transactional
    @CacheEvict(value = "category", allEntries = true)
    public void removeMenuByIds(List<Long> list) {
        List<Long> rootIds = list == null ? List.of()
                : list.stream().filter(Objects::nonNull).distinct().toList();
        if (rootIds.isEmpty()) {
            return;
        }

        // 不带 show_status 过滤：隐藏的分类也要能删
        Map<Long, CategoryEntity> allById = baseMapper.selectList(null).stream()
                .collect(Collectors.toMap(CategoryEntity::getCatId, category -> category));

        // 查不到的 id 直接跳过，删除是幂等的
        List<Long> realRootIds = rootIds.stream().filter(allById::containsKey).toList();
        if (realRootIds.isEmpty()) {
            return;
        }

        List<Long> subtreeIds = collectSubtree(realRootIds, allById).stream()
                .map(CategoryEntity::getCatId).toList();

        ensureNoReference(realRootIds, subtreeIds, allById);

        baseMapper.deleteByIds(subtreeIds);
        log.info("级联删除分类完成：根节点={}，含子分类共 {} 个，ids={}",
                realRootIds, subtreeIds.size(), subtreeIds);
    }

    /**
     * 收集这些分类及其全部后代的实体。引用分类的数据都挂在叶子上，所以校验要覆盖整棵子树。
     * 用队列而非递归：parent_cid 没有外键约束，成环时递归会栈溢出；visited 兼做去重。
     */
    private List<CategoryEntity> collectSubtree(List<Long> rootIds, Map<Long, CategoryEntity> allById) {
        // groupingBy 不接受 null key，parent_cid 为空的脏行先剔掉
        Map<Long, List<CategoryEntity>> childrenByParentCid = allById.values().stream()
                .filter(category -> category.getParentCid() != null)
                .collect(Collectors.groupingBy(CategoryEntity::getParentCid));

        List<CategoryEntity> result = new ArrayList<>();
        Set<Long> visited = new HashSet<>();
        Deque<Long> pending = new ArrayDeque<>(rootIds);
        while (!pending.isEmpty()) {
            Long catId = pending.poll();
            if (!visited.add(catId)) {
                continue;
            }
            result.add(allById.get(catId));
            childrenByParentCid.getOrDefault(catId, List.of())
                    .forEach(child -> pending.add(child.getCatId()));
        }
        return result;
    }

    /**
     * 分类下挂着品牌关联、商品、属性组或属性时拒绝删除。
     *
     * <p>品牌关联也拦而不是清：那些品牌还活着，静默清掉就等于让它们悄悄丢掉"归属哪个分类"，
     * 之后建商品时选不出来，而这中间没有任何人被告知。</p>
     *
     * <p>子分类是唯一的例外，它是这个分类自己的组合子记录，跟着一起删。</p>
     */
    private void ensureNoReference(List<Long> rootIds, List<Long> subtreeIds,
                                   Map<Long, CategoryEntity> allById) {
        List<String> blockers = new ArrayList<>();

        addBlocker(blockers, "条品牌关联（到品牌页「关联分类」里移除）", categoryBrandRelationService.count(
                new LambdaQueryWrapper<CategoryBrandRelationEntity>()
                        .in(CategoryBrandRelationEntity::getCatalogId, subtreeIds)));
        addBlocker(blockers, "个商品", spuInfoDao.selectCount(
                new LambdaQueryWrapper<SpuInfoEntity>().in(SpuInfoEntity::getCatalogId, subtreeIds)));
        addBlocker(blockers, "个属性组", attrGroupDao.selectCount(
                new LambdaQueryWrapper<AttrGroupEntity>().in(AttrGroupEntity::getCatalogId, subtreeIds)));
        addBlocker(blockers, "个属性", attrDao.selectCount(
                new LambdaQueryWrapper<AttrEntity>().in(AttrEntity::getCatalogId, subtreeIds)));

        if (blockers.isEmpty()) {
            return;
        }

        String names = rootIds.stream()
                .map(id -> allById.get(id).getName())
                .collect(Collectors.joining("、"));
        throw new BaseException(BaseCodeEnum.CATEGORY_IN_USE,
                "分类【" + names + "】及其子分类下还有 " + String.join("、", blockers) + "，请先处理后再删除");
    }

    private static void addBlocker(List<String> blockers, String unit, Long count) {
        if (count != null && count > 0) {
            blockers.add(count + " " + unit);
        }
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
     * 前台首页/全局导航的分类树，只含 show_status = 1 的分类。
     */
    @Cacheable(value = "category", key = "#root.method.name", sync = true)
    @Override
    public List<CategoryVo> getCatalogTree() {
        List<CategoryEntity> selectList = this.baseMapper.selectList(
                new LambdaQueryWrapper<CategoryEntity>().eq(CategoryEntity::getShowStatus, 1));

        // 按 parentCid 分组，一次遍历代替每层都 stream().filter() 扫一遍全表。
        // groupingBy 不接受 null key，parent_cid 为空的脏行先剔掉
        Map<Long, List<CategoryEntity>> childrenByParentCid = selectList.stream()
                .filter(category -> category.getParentCid() != null)
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