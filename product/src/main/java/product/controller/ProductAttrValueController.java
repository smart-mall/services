package product.controller;

import common.vo.PageVO;
import common.utils.R;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import product.entity.ProductAttrValueEntity;
import product.service.ProductAttrValueService;

import java.util.Arrays;
import java.util.Map;


import common.query.PageQuery;
/**
 * 商品规格参数值的后台管理接口：分页列表、详情、新增、修改、删除。
 *
 * <p>路径不在 {@code /front} 下，经网关访问时按管理端接口鉴权；按 spu 查询与全量覆盖走
 * {@link AttrController}。
 */
@RestController
@RequestMapping("product/productattrvalue")
public class ProductAttrValueController {
    @Autowired
    private ProductAttrValueService productAttrValueService;

    /**
     * 分页查询商品规格参数值。
     *
     * @param query 分页参数，{@code page} 为页码、{@code limit} 为每页条数
     * @return 分页结果，{@code rows} 为规格参数值列表
     */
    @RequestMapping("/list")
    public R<PageVO<ProductAttrValueEntity>> list(PageQuery query){
        PageVO<ProductAttrValueEntity> page = productAttrValueService.queryPage(query);

        return R.ok(page);
    }


    /**
     * 按主键查询规格参数值详情。
     *
     * @param id 规格参数值主键
     * @return 规格参数值详情；不存在时 {@code data} 为 {@code null}
     */
    @RequestMapping("/info/{id}")
    public R<ProductAttrValueEntity> info(@PathVariable("id") Long id){
		ProductAttrValueEntity productAttrValue = productAttrValueService.getById(id);

        return R.ok(productAttrValue);
    }

    /**
     * 新增一条商品规格参数值。
     *
     * @param productAttrValue 规格参数值内容，需带 {@code spuId} 与 {@code attrId}
     * @return 统一成功响应，不含业务数据
     */
    @RequestMapping("/save")
    public R<Void> save(@RequestBody ProductAttrValueEntity productAttrValue){
		productAttrValueService.save(productAttrValue);

        return R.ok();
    }

    /**
     * 按主键修改规格参数值。
     *
     * @param productAttrValue 规格参数值内容，主键必填
     * @return 统一成功响应，不含业务数据
     */
    @RequestMapping("/update")
    public R<Void> update(@RequestBody ProductAttrValueEntity productAttrValue){
		productAttrValueService.updateById(productAttrValue);

        return R.ok();
    }

    /**
     * 按主键批量删除规格参数值。
     *
     * @param ids 待删除的规格参数值主键数组
     * @return 统一成功响应，不含业务数据
     */
    @RequestMapping("/delete")
    public R<Void> delete(@RequestBody Long[] ids){
		productAttrValueService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

}
