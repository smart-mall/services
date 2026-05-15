package product.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import common.utils.PageUtils;
import common.utils.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import product.entity.ProductAttrValueEntity;
import product.service.AttrService;
import product.service.ProductAttrValueService;
import product.vo.AttrRespVO;
import product.vo.AttrVO;

import java.util.Arrays;
import java.util.List;
import java.util.Map;


/**
 * 商品属性
 *
 * @author ??
 * @email sunlightcs@gmail.com
 * @date 2025-09-15 09:58:33
 */
@RestController
@RequestMapping("product/attr")
@Slf4j
public class AttrController {
    private final AttrService attrService;

    private final ProductAttrValueService productAttrValueService;

    public AttrController(AttrService attrService, ProductAttrValueService productAttrValueService) {
        this.attrService = attrService;
        this.productAttrValueService = productAttrValueService;
    }


    /**
     * 查询商品的规格属性
     */
    @GetMapping("/base/listforspu/{spuId}")
    public R baseAttrListForSpu(@PathVariable Long spuId) {
        log.info("通过spuId查询商品规格属性：{}", spuId);
        List<ProductAttrValueEntity> productAttrValueEntities = productAttrValueService.baseAttrListForSpu(spuId);
        return R.ok().put("data",productAttrValueEntities);
    }

    /**
     * 列表
     */
    @RequestMapping("/{attrType}/list/{category}")
    public R baseAttrList(@RequestParam Map<String, Object> params, @PathVariable Long category, @PathVariable String attrType) {
        log.info("查询商品属性：{}--{}--{}", JSON.toJSONString(params, SerializerFeature.PrettyFormat), category, attrType);
        PageUtils page = attrService.queryBaseAttrPage(params, category, attrType);

        return R.ok().put("page", page);
    }

    /**
     * 列表
     */
    @RequestMapping("/list")
    public R list(@RequestParam Map<String, Object> params) {
        log.info("查询商品属性：{}", JSON.toJSONString(params, SerializerFeature.PrettyFormat));
        PageUtils page = attrService.queryPage(params);

        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{attrId}")
    public R info(@PathVariable("attrId") Long attrId) {
        log.info("根据id查询商品属性：{}", attrId);
        AttrRespVO attr = attrService.getAttrInfo(attrId);

        return R.ok().put("attr", attr);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    public R save(@RequestBody AttrVO attr) {
        log.info("保存商品属性：{}", attr);
        attrService.saveAttr(attr);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    public R update(@RequestBody AttrVO attr) {
        log.info("修改商品属性：{}", attr);
        attrService.updateAttr(attr);

        return R.ok();
    }

    /**
     * 修改商品规格
     */
    @RequestMapping("/update/{spuId}")
    public R update(@RequestBody List<ProductAttrValueEntity> entities, @PathVariable Long spuId) {
        log.info("修改商品规格：{}--{}", entities, spuId);
        productAttrValueService.updateSpuAttr(spuId, entities);
        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    public R delete(@RequestBody Long[] attrIds) {
        log.info("删除商品属性：{}", JSON.toJSONString(attrIds, SerializerFeature.PrettyFormat));
        attrService.deleteByIds(Arrays.asList(attrIds));

        return R.ok();
    }

}
