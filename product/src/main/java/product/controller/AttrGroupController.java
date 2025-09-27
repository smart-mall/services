package product.controller;

import common.utils.PageUtils;
import common.utils.R;
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
    @GetMapping("/{catelogId}/withattr")
    public R getAttrGroupWithAttrs(@PathVariable Long catelogId) {
        List<AttrGroupWithAttrsVO> list = attrGroupService.getAttrGroupWithAttrs(catelogId);

        return R.ok().put("data", list);
    }

    /**
     * 获取分组的所有属性
     */
    @GetMapping("/{attrGroupId}/attr/relation")
    public R attrRelation(@PathVariable Long attrGroupId) {
        List<AttrEntity> list = attrService.getRelationAttr(attrGroupId);
        return R.ok().put("data", list);
    }

    /**
     * 获取分组的所有属性
     */
    @GetMapping("/{attrGroupId}/noattr/relation")
    public R attrNoRelation(@RequestParam Map<String, Object> params,
                            @PathVariable Long attrGroupId) {
        PageUtils pageUtils = attrService.getNoRelationAttr(attrGroupId, params);
        return R.ok().put("page", pageUtils);
    }

    /**
     * 添加分组下的属性
     */
    @PostMapping("/attr/relation")
    public R addRelation(@RequestBody List<AttrGroupRelationVO> vos) {
        relationService.addRelation(vos);
        return R.ok();
    }


    /**
     * 删除分组下的属性
     */
    @PostMapping("/attr/relation/delete")
    public R deleteRelation(@RequestBody AttrGroupRelationVO[] vos) {
        attrGroupService.deleteRelation(vos);
        return R.ok();
    }



    /**
     * 列表
     */
    @RequestMapping("/list/{categoryId}")
    public R list(@RequestParam Map<String, Object> params, @PathVariable Long categoryId){
        PageUtils page = attrGroupService.queryPage(params, categoryId);

        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{attrGroupId}")
    public R info(@PathVariable("attrGroupId") Long attrGroupId){
		AttrGroupEntity attrGroup = attrGroupService.getById(attrGroupId);


        List<Long> catelogIds = categoryService.findCatelogIds(attrGroup.getCatelogId());
        attrGroup.setCatelogIds(catelogIds);

        return R.ok().put("attrGroup", attrGroup);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    public R save(@RequestBody AttrGroupEntity attrGroup){
		attrGroupService.save(attrGroup);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    public R update(@RequestBody AttrGroupEntity attrGroup){
		attrGroupService.updateById(attrGroup);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    public R delete(@RequestBody Long[] attrGroupIds){
		attrGroupService.removeByIds(Arrays.asList(attrGroupIds));

        return R.ok();
    }

}
