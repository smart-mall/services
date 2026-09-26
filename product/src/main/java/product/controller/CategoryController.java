package product.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import common.utils.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.web.bind.annotation.*;
import product.entity.CategoryEntity;
import product.service.CategoryService;

import java.util.Arrays;
import java.util.List;


/**
 * 商品三级分类后台管理接口：菜单树、详情、增删改与批量更新排序。
 *
 * <p>路径不在 {@code /front} 下，经网关访问时按管理端接口鉴权；前台首页的分类树走
 * {@link product.web.WebController}。
 *
 * <p>写操作都会清空 {@code category} 缓存，前台分类树与本接口共用这一份缓存。
 */
@RestController
@RequestMapping("product/category")
@Slf4j
public class CategoryController {
    @Autowired
    private CategoryService categoryService;

    /**
     * 返回全部三级分类的树形结构。
     *
     * <p>后台菜单树不按 {@code showStatus} 过滤，隐藏的分类也会出现；一级分类与各级子分类均按
     * {@code sort} 升序。
     *
     * @return 一级分类列表，子分类通过 {@code children} 嵌套
     */
    @RequestMapping("/list/tree")
    public R<List<CategoryEntity>> list(){
        log.info("查询所有分类");
        List<CategoryEntity> categoryEntities =  categoryService.listWithTree();

        return R.ok(categoryEntities);
    }


    /**
     * 按主键查询分类详情。
     *
     * @param catId 分类 ID
     * @return 分类详情；不存在时 {@code data} 为 {@code null}
     */
    @RequestMapping("/info/{catId}")
    public R<CategoryEntity> info(@PathVariable("catId") Long catId){
        log.info("查询分类数据{}", catId);
		CategoryEntity category = categoryService.getById(catId);

        return R.ok(category);
    }

    /**
     * 新增分类。
     *
     * <p>不传「是否显示」时默认置为显示；写入后清空 {@code category} 缓存。
     *
     * @param category 分类内容，主键由数据库生成
     * @return 统一成功响应，不含业务数据
     */
    @RequestMapping("/save")
    @CacheEvict(value = "category", allEntries = true)
    public R<Void> save(@RequestBody CategoryEntity category){
        if (category.getShowStatus() == null) {
            category.setShowStatus(1);
        }
        log.info("保存分类数据{}", category);
		categoryService.save(category);

        return R.ok();
    }

    /**
     * 修改分类。
     *
     * <p>同步更新品牌分类关联表里冗余的分类名，并清空 {@code category} 缓存。
     *
     * @param category 分类内容，主键必填
     * @return 统一成功响应，不含业务数据
     */
    @RequestMapping("/update")
    @CacheEvict(value = "category", allEntries = true)
    public R<Void> update(@RequestBody CategoryEntity category){
        log.info("修改分类数据{}", category);
		categoryService.updateDetail(category);

        return R.ok();
    }

    /**
     * 按主键批量更新分类，用于保存调整后的排序值。
     *
     * <p>写入后清空 {@code category} 缓存。
     *
     * @param category 分类数组，每项需带主键与新排序值 {@code sort}
     * @return 统一成功响应，不含业务数据
     */
    @RequestMapping("/update/sort")
    @CacheEvict(value = "category", allEntries = true)
    public R<Void> updateSort(@RequestBody CategoryEntity[] category){
        log.info("批量修改菜单{}", JSON.toJSONString(category, SerializerFeature.PrettyFormat));
        categoryService.updateBatchById(Arrays.asList(category));
        return R.ok();
    }


    /**
     * 删除分类，连同其全部子分类一起物理删除。
     *
     * <p>子树下还挂着商品、属性组、属性或品牌关联时整批拒绝，需要先处理；删除后清空
     * {@code category} 缓存。
     *
     * @param catIds 待删除的分类主键数组，传不存在的 ID 会被静默跳过
     * @return 统一成功响应，不含业务数据
     */
    @RequestMapping("/delete")
    @CacheEvict(value = "category", allEntries = true)
    public R<Void> delete(@RequestBody Long[] catIds){
        log.info("删除分类数据{}",JSON.toJSONString(catIds, SerializerFeature.PrettyFormat));
		categoryService.removeMenuByIds(Arrays.asList(catIds));

        return R.ok();
    }

}
