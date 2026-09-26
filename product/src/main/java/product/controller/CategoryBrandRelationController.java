package product.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import common.vo.PageVO;
import common.utils.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import product.entity.BrandEntity;
import product.entity.CategoryBrandRelationEntity;
import product.service.CategoryBrandRelationService;
import product.vo.BrandVO;

import java.util.Arrays;
import java.util.List;
import java.util.Map;


import common.query.PageQuery;
/**
 * 品牌分类关联
 */
@RestController
@RequestMapping("product/categorybrandrelation")
@Slf4j
public class CategoryBrandRelationController {
    private final CategoryBrandRelationService categoryBrandRelationService;

    public CategoryBrandRelationController(CategoryBrandRelationService categoryBrandRelationService) {
        this.categoryBrandRelationService = categoryBrandRelationService;
    }

    /**
     * 获取分类关联列表
     */
    @GetMapping("/catalog/list")
    public R<List<CategoryBrandRelationEntity>> catalogList(@RequestParam Long brandId){
        log.info("根据品牌获取分类关联列表：{}", brandId);
        List<CategoryBrandRelationEntity> list = categoryBrandRelationService.listCategoryBrandRelation(brandId);

        return R.ok(list);
    }

    /**
     * 获取分类品牌关联表
     */
    @GetMapping("/brands/list")
    public R<List<BrandVO>> relationBrandList(@RequestParam Long catId){
        log.info("根据分类获取分类品牌关联表：{}", catId);
        List<BrandEntity> list = categoryBrandRelationService.getBrandByCatId(catId);

        List<BrandVO> data = list.stream().map(item -> {
            BrandVO brandVO = new BrandVO();
            brandVO.setBrandId(item.getBrandId());
            brandVO.setName(item.getName());

            return brandVO;
        }).toList();

        return R.ok(data);
    }



    /**
     * 列表
     */
    @RequestMapping("/list")
    public R<PageVO<CategoryBrandRelationEntity>> list(PageQuery query){
        log.info("获取分类品牌关联表；{}", JSON.toJSONString(query, SerializerFeature.PrettyFormat));
        PageVO<CategoryBrandRelationEntity> page = categoryBrandRelationService.queryPage(query);

        return R.ok(page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    public R<CategoryBrandRelationEntity> info(@PathVariable("id") Long id){
        log.info("通过id获取分类品牌关联表: {}", id);
		CategoryBrandRelationEntity categoryBrandRelation = categoryBrandRelationService.getById(id);

        return R.ok(categoryBrandRelation);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    public R<Void> save(@RequestBody CategoryBrandRelationEntity categoryBrandRelation){
        log.info("保存：{}", categoryBrandRelation);
		categoryBrandRelationService.saveDetail(categoryBrandRelation);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    public R<Void> update(@RequestBody CategoryBrandRelationEntity categoryBrandRelation){
        log.info("更新：{}", categoryBrandRelation);

        categoryBrandRelationService.updateById(categoryBrandRelation);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    public R<Void> delete(@RequestBody Long[] ids){
        log.info("删除：{}", JSON.toJSONString(ids, SerializerFeature.PrettyFormat));
		categoryBrandRelationService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

}
