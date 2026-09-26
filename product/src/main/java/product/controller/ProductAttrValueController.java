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
 * spu属性值
 */
@RestController
@RequestMapping("product/productattrvalue")
public class ProductAttrValueController {
    @Autowired
    private ProductAttrValueService productAttrValueService;

    /**
     * 列表
     */
    @RequestMapping("/list")
    public R<PageVO<ProductAttrValueEntity>> list(PageQuery query){
        PageVO<ProductAttrValueEntity> page = productAttrValueService.queryPage(query);

        return R.ok(page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    public R<ProductAttrValueEntity> info(@PathVariable("id") Long id){
		ProductAttrValueEntity productAttrValue = productAttrValueService.getById(id);

        return R.ok(productAttrValue);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    public R<Void> save(@RequestBody ProductAttrValueEntity productAttrValue){
		productAttrValueService.save(productAttrValue);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    public R<Void> update(@RequestBody ProductAttrValueEntity productAttrValue){
		productAttrValueService.updateById(productAttrValue);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    public R<Void> delete(@RequestBody Long[] ids){
		productAttrValueService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

}
