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


import common.query.PageQuery;
/**
 * 品牌与分类关联的后台管理接口：按品牌或分类查关联、分页列表、详情、新增、修改、删除。
 *
 * <p>路径不在 {@code /front} 下，经网关访问时按管理端接口鉴权。
 */
@RestController
@RequestMapping("product/categorybrandrelation")
@Slf4j
public class CategoryBrandRelationController {
    private final CategoryBrandRelationService categoryBrandRelationService;

    /**
     * 由 Spring 注入品牌分类关联服务，创建后即可直接调用。
     *
     * @param categoryBrandRelationService 品牌分类关联服务
     */
    public CategoryBrandRelationController(CategoryBrandRelationService categoryBrandRelationService) {
        this.categoryBrandRelationService = categoryBrandRelationService;
    }

    /**
     * 查询某品牌关联的全部分类。
     *
     * @param brandId 品牌 ID
     * @return 该品牌的分类关联行，含冗余的分类名；没有关联时返回空列表
     */
    @GetMapping("/catalog/list")
    public R<List<CategoryBrandRelationEntity>> catalogList(@RequestParam Long brandId){
        log.info("根据品牌获取分类关联列表：{}", brandId);
        List<CategoryBrandRelationEntity> list = categoryBrandRelationService.listCategoryBrandRelation(brandId);

        return R.ok(list);
    }

    /**
     * 查询某分类下关联的全部品牌。
     *
     * @param catId 三级分类 ID
     * @return 品牌列表，只带 {@code brandId} 与品牌名；没有关联时返回空列表
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
     * 分页查询全部品牌分类关联。
     *
     * @param query 分页参数，{@code page} 为页码、{@code limit} 为每页条数
     * @return 分页结果，{@code rows} 为关联行列表
     */
    @RequestMapping("/list")
    public R<PageVO<CategoryBrandRelationEntity>> list(PageQuery query){
        log.info("获取分类品牌关联表；{}", JSON.toJSONString(query, SerializerFeature.PrettyFormat));
        PageVO<CategoryBrandRelationEntity> page = categoryBrandRelationService.queryPage(query);

        return R.ok(page);
    }


    /**
     * 按主键查询关联详情。
     *
     * @param id 关联行主键
     * @return 关联详情；不存在时 {@code data} 为 {@code null}
     */
    @RequestMapping("/info/{id}")
    public R<CategoryBrandRelationEntity> info(@PathVariable("id") Long id){
        log.info("通过id获取分类品牌关联表: {}", id);
		CategoryBrandRelationEntity categoryBrandRelation = categoryBrandRelationService.getById(id);

        return R.ok(categoryBrandRelation);
    }

    /**
     * 新增品牌与分类的关联。
     *
     * <p>入参只需给 {@code brandId} 与 {@code catalogId}，冗余的品牌名与分类名由服务端回查填入。
     *
     * @param categoryBrandRelation 关联内容，{@code brandId} 与 {@code catalogId} 必填
     * @return 统一成功响应，不含业务数据
     */
    @RequestMapping("/save")
    public R<Void> save(@RequestBody CategoryBrandRelationEntity categoryBrandRelation){
        log.info("保存：{}", categoryBrandRelation);
		categoryBrandRelationService.saveDetail(categoryBrandRelation);

        return R.ok();
    }

    /**
     * 按主键修改关联行。
     *
     * <p>直接更新入参字段，不会像新增那样回查并刷新冗余的品牌名与分类名。
     *
     * @param categoryBrandRelation 关联内容，主键必填
     * @return 统一成功响应，不含业务数据
     */
    @RequestMapping("/update")
    public R<Void> update(@RequestBody CategoryBrandRelationEntity categoryBrandRelation){
        log.info("更新：{}", categoryBrandRelation);

        categoryBrandRelationService.updateById(categoryBrandRelation);

        return R.ok();
    }

    /**
     * 按主键批量删除关联行。
     *
     * @param ids 待删除的关联行主键数组
     * @return 统一成功响应，不含业务数据
     */
    @RequestMapping("/delete")
    public R<Void> delete(@RequestBody Long[] ids){
        log.info("删除：{}", JSON.toJSONString(ids, SerializerFeature.PrettyFormat));
		categoryBrandRelationService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

}
