package product.controller;

import common.vo.PageVO;
import common.utils.R;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import product.entity.SkuSaleAttrValueEntity;
import product.service.SkuSaleAttrValueService;

import java.util.Arrays;
import java.util.List;
import java.util.Map;


import common.query.PageQuery;
/**
 * sku 销售属性值的后台管理接口：按 sku 取销售属性，以及分页列表、详情、新增、修改、删除。
 *
 * <p>路径不在 {@code /front} 下，经网关访问时按管理端接口鉴权。
 */
@RestController
@RequestMapping("product/skusaleattrvalue")
public class SkuSaleAttrValueController {
    @Autowired
    private SkuSaleAttrValueService skuSaleAttrValueService;

    /**
     * 按 sku 取销售属性的字符串表示。
     *
     * <p>直接返回字符串列表，不套 {@code R} 外壳。
     *
     * @param skuId sku ID
     * @return 形如 {@code 颜色：白色} 的字符串列表；没有销售属性时返回空列表
     */
    @GetMapping(value = "/stringList/{skuId}")
    public List<String> getSkuSaleAttrValues(@PathVariable("skuId") Long skuId) {
        return skuSaleAttrValueService.getSkuSaleAttrValuesAsStringList(skuId);
    }

    /**
     * 分页查询 sku 销售属性值。
     *
     * @param query 分页参数，{@code page} 为页码、{@code limit} 为每页条数
     * @return 分页结果，{@code rows} 为销售属性值列表
     */
    @RequestMapping("/list")
    public R<PageVO<SkuSaleAttrValueEntity>> list(PageQuery query){
        PageVO<SkuSaleAttrValueEntity> page = skuSaleAttrValueService.queryPage(query);

        return R.ok(page);
    }


    /**
     * 按主键查询销售属性值详情。
     *
     * @param id 销售属性值主键
     * @return 销售属性值详情；不存在时 {@code data} 为 {@code null}
     */
    @RequestMapping("/info/{id}")
    public R<SkuSaleAttrValueEntity> info(@PathVariable("id") Long id){
		SkuSaleAttrValueEntity skuSaleAttrValue = skuSaleAttrValueService.getById(id);

        return R.ok(skuSaleAttrValue);
    }

    /**
     * 新增一条 sku 销售属性值。
     *
     * @param skuSaleAttrValue 销售属性值内容，需带 {@code skuId} 与 {@code attrId}
     * @return 统一成功响应，不含业务数据
     */
    @RequestMapping("/save")
    public R<Void> save(@RequestBody SkuSaleAttrValueEntity skuSaleAttrValue){
		skuSaleAttrValueService.save(skuSaleAttrValue);

        return R.ok();
    }

    /**
     * 按主键修改销售属性值。
     *
     * @param skuSaleAttrValue 销售属性值内容，主键必填
     * @return 统一成功响应，不含业务数据
     */
    @RequestMapping("/update")
    public R<Void> update(@RequestBody SkuSaleAttrValueEntity skuSaleAttrValue){
		skuSaleAttrValueService.updateById(skuSaleAttrValue);

        return R.ok();
    }

    /**
     * 按主键批量删除销售属性值。
     *
     * @param ids 待删除的销售属性值主键数组
     * @return 统一成功响应，不含业务数据
     */
    @RequestMapping("/delete")
    public R<Void> delete(@RequestBody Long[] ids){
		skuSaleAttrValueService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

}
