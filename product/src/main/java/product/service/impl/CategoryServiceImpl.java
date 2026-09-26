package product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import common.exception.BaseCodeEnum;
import common.exception.BaseException;
import common.vo.PageVO;
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

import common.query.PageQuery;
/**
 * 商品三级分类服务的默认实现，基于 MyBatis-Plus 的 {@code ServiceImpl} 读写 {@code pms_category}。
 *
 * <p>分类树由 {@code parent_cid} 自关联拼出，删除时连同整棵子树物理删除；前台分类树带
 * {@code category} 缓存，删除分类后整体清除。
 */
@Slf4j
@Service("categoryService")
public class CategoryServiceImpl extends ServiceImpl<CategoryDao, CategoryEntity> implements CategoryService {
    private final CategoryBrandRelationService categoryBrandRelationService;

    // 注入 DAO 而不是对应的 Service：那三个 Service 都依赖 CategoryService，会构造器循环
    private final SpuInfoDao spuInfoDao;
    private final AttrDao attrDao;
    private final AttrGroupDao attrGroupDao;

    /**
     * 由容器注入品牌分类关联服务与三张引用表的 Mapper 构造。
     *
     * @param categoryBrandRelationService 品牌分类关联服务，分类改名后回写关联表里的分类名
     * @param spuInfoDao spu 主表 Mapper，删除分类前查子树下是否还有商品
     * @param attrDao 属性 Mapper，删除分类前查子树下是否还有属性
     * @param attrGroupDao 属性分组 Mapper，删除分类前查子树下是否还有属性分组
     */
    public CategoryServiceImpl(CategoryBrandRelationService categoryBrandRelationService,
                               SpuInfoDao spuInfoDao,
                               AttrDao attrDao,
                               AttrGroupDao attrGroupDao) {
        this.categoryBrandRelationService = categoryBrandRelationService;
        this.spuInfoDao = spuInfoDao;
        this.attrDao = attrDao;
        this.attrGroupDao = attrGroupDao;
    }

    /** {@inheritDoc} */
    @Override
    public PageVO<CategoryEntity> queryPage(PageQuery query) {
        IPage<CategoryEntity> page = this.page(query.toPage());

        return new PageVO<>(page.getTotal(), page.getRecords());
    }

    /** {@inheritDoc} */
    @Override
    public List<CategoryEntity> listWithTree() {
        // 1. 查出全部分类，按 id 建索引：下面每个节点都要找父节点，逐个扫列表是 O(n²)
        List<CategoryEntity> categoryEntities = baseMapper.selectList(null);

        Map<Long, CategoryEntity> categoryEntityMap = categoryEntities.stream()
                .collect(Collectors.toMap(CategoryEntity::getCatId, v -> v));

        // 2. 一级分类（parent_cid = 0）就是返回的根列表
        List<CategoryEntity> level1Menus = categoryEntities.stream()
                .filter(categoryEntity -> categoryEntity.getParentCid() == 0)
                .sorted(Comparator.comparingInt(CategoryEntity::getSort))
                .collect(Collectors.toList());

        // 3. 把每个非一级分类挂到父节点的 children 上
        categoryEntities.forEach(categoryEntity -> {
            if (categoryEntity.getParentCid() == 0) {
                return;
            }
            CategoryEntity categoryParent = categoryEntityMap.get(categoryEntity.getParentCid());
            if (categoryParent != null) {
                if (categoryParent.getChildren() == null) {
                    categoryParent.setChildren(new ArrayList<>());
                }
                categoryParent.getChildren().add(categoryEntity);
            }
        });

        // 4. 子节点是按入库顺序挂上的，逐层按 sort 重排
        level1Menus.forEach(this::sortChildren);

        return level1Menus;
    }

    /**
     * 递归把子节点按 {@code sort} 升序重排。
     *
     * @param category 当前节点，{@code children} 为 {@code null} 或空时直接返回
     */
    private void sortChildren(CategoryEntity category) {
        if (category.getChildren() != null && !category.getChildren().isEmpty()) {
            category.getChildren().sort(Comparator.comparingInt(CategoryEntity::getSort));
            category.getChildren().forEach(this::sortChildren);
        }
    }

    /** {@inheritDoc} */
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
     * 收集这些分类及其全部后代的实体，引用分类的数据都挂在叶子上，所以校验要覆盖整棵子树。
     *
     * <p>用队列而非递归：{@code parent_cid} 没有外键约束，成环时递归会栈溢出；{@code visited}
     * 兼做去重。
     *
     * @param rootIds 待删除的根分类 ID，不能为空
     * @param allById 全部分类，按 {@code catId} 建索引
     * @return 这些分类连同全部后代的实体；查不到的 ID 会被跳过
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
     * 之后建商品时选不出来。子分类是唯一的例外，它是这个分类自己的组合子记录，跟着一起删。
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

    /** {@inheritDoc} */
    @Override
    public List<Long> findcatalogIds(Long catId) {
        // 路径由递归逐级 add 拼出：父级先追加、本级最后追加，所以必须是可变集合
        List<Long> path = new LinkedList<>();
        findParentPath(catId, path);
        return path;
    }

    /**
     * {@inheritDoc}
     *
     * <p>分类表与品牌分类关联表的更新在同一个事务里，不会只改一半。
     *
     * <p>改名会让 {@code category} 缓存里的分类树过期，所以同时清空该缓存。
     */
    @Override
    @Transactional
    @CacheEvict(value = "category", allEntries = true)
    public void updateDetail(CategoryEntity category) {
        log.debug("先修改分类表");
        this.updateById(category);
        log.debug("修改品牌分类关联表");
        categoryBrandRelationService.updateCategory(category.getCatId(), category.getName());
    }

    /** {@inheritDoc} */
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
     * 递归把分类实体组装成 {@link CategoryVo} 树，每一层都按 {@code sort} 升序。
     *
     * @param childrenByParentCid 已经按 {@code parentCid} 分好组的全部分类
     * @param parentCid 当前要组装的父分类 ID，一级分类传 0
     * @return 该父分类下的子节点列表；没有子分类时返回空列表
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
     * 递归把从一级分类到 {@code catalogId} 的分类 ID 追加进 {@code path}。
     *
     * @param catalogId 当前分类 ID，为 {@code null} 或查不到时直接返回
     * @param path 承接路径的可变集合，父级先追加、本级最后追加
     */
    private void findParentPath(Long catalogId, List<Long> path) {
        // 1. 分类不存在就没有路径可拼，直接返回
        if (catalogId == null) {
            return;
        }
        CategoryEntity categoryEntity = baseMapper.selectById(catalogId);
        if (categoryEntity == null) {
            return;
        }

        // 2. 先递归父级：父级全部追加完再追加本级，路径顺序才是顶级到本级
        if (categoryEntity.getParentCid() != 0) {
            findParentPath(categoryEntity.getParentCid(), path);
        }

        // 3. 追加本级
        path.add(categoryEntity.getCatId());
    }
}