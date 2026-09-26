package product.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import common.vo.PageVO;
import common.utils.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import product.entity.AttrEntity;
import product.entity.ProductAttrValueEntity;
import product.service.AttrService;
import product.service.ProductAttrValueService;
import product.vo.AttrRespVO;
import product.vo.AttrVO;

import java.util.Arrays;
import java.util.List;
import java.util.Map;


import common.query.PageQuery;
import common.query.KeyPageQuery;
/**
 * 商品属性
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
    public R<List<ProductAttrValueEntity>> baseAttrListForSpu(@PathVariable Long spuId) {
        log.info("通过spuId查询商品规格属性：{}", spuId);
        List<ProductAttrValueEntity> productAttrValueEntities = productAttrValueService.baseAttrListForSpu(spuId);
        return R.ok(productAttrValueEntities);
    }

    /**
     * 列表
     */
    @RequestMapping("/{attrType}/list/{category}")
    public R<PageVO<AttrRespVO>> baseAttrList(KeyPageQuery query, @PathVariable Long category, @PathVariable String attrType) {
        log.info("查询商品属性：{}--{}--{}", JSON.toJSONString(query, SerializerFeature.PrettyFormat), category, attrType);
        PageVO<AttrRespVO> page = attrService.queryBaseAttrPage(query, category, attrType);

        return R.ok(page);
    }

    /**
     * 列表
     */
    @RequestMapping("/list")
    public R<PageVO<AttrEntity>> list(PageQuery query) {
        log.info("查询商品属性：{}", JSON.toJSONString(query, SerializerFeature.PrettyFormat));
        PageVO<AttrEntity> page = attrService.queryPage(query);

        return R.ok(page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{attrId}")
    public R<AttrRespVO> info(@PathVariable("attrId") Long attrId) {
        log.info("根据id查询商品属性：{}", attrId);
        AttrRespVO attr = attrService.getAttrInfo(attrId);

        return R.ok(attr);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    public R<Void> save(@RequestBody AttrVO attr) {
        log.info("保存商品属性：{}", attr);
        attrService.saveAttr(attr);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    public R<Void> update(@RequestBody AttrVO attr) {
        log.info("修改商品属性：{}", attr);
        attrService.updateAttr(attr);

        return R.ok();
    }

    /**
     * 修改商品规格
     */
    @RequestMapping("/update/{spuId}")
    public R<Void> update(@RequestBody List<ProductAttrValueEntity> entities, @PathVariable Long spuId) {
        log.info("修改商品规格：{}--{}", entities, spuId);
        productAttrValueService.updateSpuAttr(spuId, entities);
        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    public R<Void> delete(@RequestBody Long[] attrIds) {
        log.info("删除商品属性：{}", JSON.toJSONString(attrIds, SerializerFeature.PrettyFormat));
        attrService.deleteByIds(Arrays.asList(attrIds));

        return R.ok();
    }

}
