package product.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import common.utils.PageUtils;
import common.utils.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import product.entity.AttrEntity;
import product.entity.AttrGroupEntity;
import product.service.AttrAttrgroupRelationService;
import product.service.AttrGroupService;
import product.service.AttrService;
import product.service.CategoryService;
import product.vo.AttrGroupRelationVO;
import product.vo.AttrGroupWithAttrsVO;

import java.util.Arrays;
import java.util.List;
import java.util.Map;


/**
 * 属性分组
 *
 * @author ??
 * @email sunlightcs@gmail.com
 * @date 2025-09-15 09:58:33
 */
@RestController
@RequestMapping("product/attrgroup")
@Slf4j
public class AttrGroupController {
    private final AttrGroupService attrGroupService;
    private final CategoryService categoryService;
    private final AttrService attrService;
    private final AttrAttrgroupRelationService relationService;

    public AttrGroupController(AttrGroupService attrGroupService, CategoryService categoryService, AttrService attrService, AttrAttrgroupRelationService relationService) {
        this.attrGroupService = attrGroupService;
        this.categoryService = categoryService;
        this.attrService = attrService;
        this.relationService = relationService;
    }

    /**
     * 根据分类id获取属性分组以及具体属性
     */
    @GetMapping("/{catalogId}/withattr")
    public R getAttrGroupWithAttrs(@PathVariable Long catalogId) {
        log.info("根据分类id获取属性分组以及具体属性：{}", catalogId);
        List<AttrGroupWithAttrsVO> list = attrGroupService.getAttrGroupWithAttrs(catalogId);

        return R.ok().put("data", list);
    }

    /**
     * 获取分组的所有属性
     */
    @GetMapping("/{attrGroupId}/attr/relation")
    public R attrRelation(@PathVariable Long attrGroupId) {
        log.info("获取分组的所有属性：{}", attrGroupId);
        List<AttrEntity> list = attrService.getRelationAttr(attrGroupId);
        return R.ok().put("data", list);
    }

    /**
     * 获取分组的所有属性
     */
    @GetMapping("/{attrGroupId}/noattr/relation")
    public R attrNoRelation(@RequestParam Map<String, Object> params,
                            @PathVariable Long attrGroupId) {
        log.info("获取分组的所有属性：{}, {}", attrGroupId, JSON.toJSONString(params, SerializerFeature.PrettyFormat));
        PageUtils pageUtils = attrService.getNoRelationAttr(attrGroupId, params);
        return R.ok().put("page", pageUtils);
    }

    /**
     * 添加分组下的属性
     */
    @PostMapping("/attr/relation")
    public R addRelation(@RequestBody List<AttrGroupRelationVO> vos) {
        log.info("添加分组下的属性：{}", JSON.toJSONString(vos, SerializerFeature.PrettyFormat));
        relationService.addRelation(vos);
        return R.ok();
    }


    /**
     * 删除分组下的属性
     */
    @PostMapping("/attr/relation/delete")
    public R deleteRelation(@RequestBody AttrGroupRelationVO[] vos) {
        log.info("删除分组下的属性：{}", JSON.toJSONString(vos, SerializerFeature.PrettyFormat));
        attrGroupService.deleteRelation(vos);
        return R.ok();
    }



    /**
     * 列表
     */
    @RequestMapping("/list/{categoryId}")
    public R list(@RequestParam Map<String, Object> params, @PathVariable Long categoryId){
        log.info("列表：{}", JSON.toJSONString(params, SerializerFeature.PrettyFormat));
        PageUtils page = attrGroupService.queryPage(params, categoryId);

        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{attrGroupId}")
    public R info(@PathVariable("attrGroupId") Long attrGroupId){
        log.info("信息：{}", attrGroupId);
		AttrGroupEntity attrGroup = attrGroupService.getById(attrGroupId);


        List<Long> catalogIds = categoryService.findcatalogIds(attrGroup.getCatalogId());
        attrGroup.setCatalogIds(catalogIds);

        return R.ok().put("attrGroup", attrGroup);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    public R save(@RequestBody AttrGroupEntity attrGroup){
        log.info("保存：{}", JSON.toJSONString(attrGroup, SerializerFeature.PrettyFormat));
		attrGroupService.save(attrGroup);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    public R update(@RequestBody AttrGroupEntity attrGroup){
        log.info("修改：{}", JSON.toJSONString(attrGroup, SerializerFeature.PrettyFormat));
		attrGroupService.updateDetail(attrGroup);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    public R delete(@RequestBody Long[] attrGroupIds){
        log.info("删除：{}", JSON.toJSONString(attrGroupIds, SerializerFeature.PrettyFormat));
		attrGroupService.deleteByIds(Arrays.asList(attrGroupIds));

        return R.ok();
    }

}
