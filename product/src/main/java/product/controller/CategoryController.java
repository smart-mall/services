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
 * 商品三级分类
 *
 * @author ??
 * @email sunlightcs@gmail.com
 * @date 2025-09-15 09:58:33
 */
@RestController
@RequestMapping("product/category")
@Slf4j
public class CategoryController {
    @Autowired
    private CategoryService categoryService;

    /**
     * 查出所有分类以及子分类，以树形结构组装起来表
     */
    @RequestMapping("/list/tree")
    public R list(){
        log.info("查询所有分类");
        List<CategoryEntity> categoryEntities =  categoryService.listWithTree();

        return R.ok().put("tree", categoryEntities);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{catId}")
    public R info(@PathVariable("catId") Long catId){
        log.info("查询分类数据{}", catId);
		CategoryEntity category = categoryService.getById(catId);

        return R.ok().put("category", category);
    }

    /**
     * 保存。不传"是否显示"时默认显示。
     */
    @RequestMapping("/save")
    @CacheEvict(value = "category", allEntries = true)
    public R save(@RequestBody CategoryEntity category){
        if (category.getShowStatus() == null) {
            category.setShowStatus(1);
        }
        log.info("保存分类数据{}", category);
		categoryService.save(category);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    @CacheEvict(value = "category", allEntries = true)
    public R update(@RequestBody CategoryEntity category){
        log.info("修改分类数据{}", category);
		categoryService.updateDetail(category);

        return R.ok();
    }

    /**
     * @description 批量修改菜单
     */
    @RequestMapping("/update/sort")
    @CacheEvict(value = "category", allEntries = true)
    public R updateSort(@RequestBody CategoryEntity[] category){
        log.info("批量修改菜单{}", JSON.toJSONString(category, SerializerFeature.PrettyFormat));
        categoryService.updateBatchById(Arrays.asList(category));
        return R.ok();
    }


    /**
     * 删除分类及其子分类；子树下还挂着商品、属性组、属性或品牌关联时整批拒绝。
     */
    @RequestMapping("/delete")
    @CacheEvict(value = "category", allEntries = true)
    public R delete(@RequestBody Long[] catIds){
        log.info("删除分类数据{}",JSON.toJSONString(catIds, SerializerFeature.PrettyFormat));
		categoryService.removeMenuByIds(Arrays.asList(catIds));

        return R.ok();
    }

}
