package product.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import common.vo.PageVO;
import product.vo.AttrGroupRespVO;
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


import common.query.KeyPageQuery;
/**
 * 属性分组后台管理接口：分组与属性的绑定维护，以及分组自身的分页列表、详情、增删改。
 *
 * <p>路径不在 {@code /front} 下，经网关访问时按管理端接口鉴权。
 */
@RestController
@RequestMapping("product/attrgroup")
@Slf4j
public class AttrGroupController {
    private final AttrGroupService attrGroupService;
    private final CategoryService categoryService;
    private final AttrService attrService;
    private final AttrAttrgroupRelationService relationService;

    /**
     * 由 Spring 注入分组、分类、属性与关联服务，创建后即可直接调用。
     *
     * @param attrGroupService 属性分组服务
     * @param categoryService 分类服务
     * @param attrService 属性服务
     * @param relationService 属性与属性分组关联服务
     */
    public AttrGroupController(AttrGroupService attrGroupService, CategoryService categoryService, AttrService attrService, AttrAttrgroupRelationService relationService) {
        this.attrGroupService = attrGroupService;
        this.categoryService = categoryService;
        this.attrService = attrService;
        this.relationService = relationService;
    }

    /**
     * 查询某三级分类下的全部属性分组，并带上每个分组已绑定的属性。
     *
     * @param catalogId 三级分类 ID
     * @return 分组列表，每组通过 {@code attrs} 携带已绑定的属性；该分类下没有分组时返回空列表
     */
    @GetMapping("/{catalogId}/withattr")
    public R<List<AttrGroupWithAttrsVO>> getAttrGroupWithAttrs(@PathVariable Long catalogId) {
        log.info("根据分类id获取属性分组以及具体属性：{}", catalogId);
        List<AttrGroupWithAttrsVO> list = attrGroupService.getAttrGroupWithAttrs(catalogId);

        return R.ok(list);
    }

    /**
     * 查询某属性分组已绑定的全部属性。
     *
     * @param attrGroupId 属性分组 ID
     * @return 已绑定到该分组的属性列表；没有绑定时返回空列表
     */
    @GetMapping("/{attrGroupId}/attr/relation")
    public R<List<AttrEntity>> attrRelation(@PathVariable Long attrGroupId) {
        log.info("获取分组的所有属性：{}", attrGroupId);
        List<AttrEntity> list = attrService.getRelationAttr(attrGroupId);
        return R.ok(list);
    }

    /**
     * 分页查询可以绑定到该分组、但尚未被绑定的属性。
     *
     * <p>排除范围是<b>同分类下所有分组</b>已绑定的属性，而不只是本分组：一个属性在同一个分类里
     * 只能属于一个分组。
     *
     * @param query 分页与关键字条件，{@code key} 同时匹配属性名与属性 ID
     * @param attrGroupId 属性分组 ID，用于定位其所属分类
     * @return 分页结果，{@code rows} 为可绑定的属性列表
     */
    @GetMapping("/{attrGroupId}/noattr/relation")
    public R<PageVO<AttrEntity>> attrNoRelation(KeyPageQuery query,
                            @PathVariable Long attrGroupId) {
        log.info("获取分组的所有属性：{}, {}", attrGroupId, JSON.toJSONString(query, SerializerFeature.PrettyFormat));
        PageVO<AttrEntity> pageUtils = attrService.getNoRelationAttr(attrGroupId, query);
        return R.ok(pageUtils);
    }

    /**
     * 批量把属性绑定到属性分组。
     *
     * <p>不校验重复：同一对 {@code attrId} + {@code attrGroupId} 重复提交会插入多行关联。
     *
     * @param vos 关联入参列表，每项需带 {@code attrId} 与 {@code attrGroupId}
     * @return 统一成功响应，不含业务数据
     */
    @PostMapping("/attr/relation")
    public R<Void> addRelation(@RequestBody List<AttrGroupRelationVO> vos) {
        log.info("添加分组下的属性：{}", JSON.toJSONString(vos, SerializerFeature.PrettyFormat));
        relationService.addRelation(vos);
        return R.ok();
    }


    /**
     * 批量解除属性与属性分组的绑定。
     *
     * <p>只删关联行，属性本身不受影响；入参为 {@code null} 时直接返回。
     *
     * @param vos 关联入参数组，每项按 {@code attrId} + {@code attrGroupId} 定位一行
     * @return 统一成功响应，不含业务数据
     */
    @PostMapping("/attr/relation/delete")
    public R<Void> deleteRelation(@RequestBody AttrGroupRelationVO[] vos) {
        log.info("删除分组下的属性：{}", JSON.toJSONString(vos, SerializerFeature.PrettyFormat));
        attrGroupService.deleteRelation(vos);
        return R.ok();
    }



    /**
     * 分页查询某分类下的属性分组，并补充分类名。
     *
     * @param query 分页与关键字条件，{@code key} 匹配分组名或分组 ID
     * @param categoryId 三级分类 ID，为 {@code null} 或 0 时不按分类过滤
     * @return 分页结果，每行含所属分类名
     */
    @RequestMapping("/list/{categoryId}")
    public R<PageVO<AttrGroupRespVO>> list(KeyPageQuery query, @PathVariable Long categoryId){
        log.info("列表：{}", JSON.toJSONString(query, SerializerFeature.PrettyFormat));
        PageVO<AttrGroupRespVO> page = attrGroupService.queryPage(query, categoryId);

        return R.ok(page);
    }


    /**
     * 按主键查询属性分组详情。
     *
     * <p>额外回填从一级分类到本分组所属分类的完整路径。
     *
     * @param attrGroupId 属性分组 ID
     * @return 分组详情，{@code catalogIds} 为分类路径
     */
    @RequestMapping("/info/{attrGroupId}")
    public R<AttrGroupEntity> info(@PathVariable("attrGroupId") Long attrGroupId){
        log.info("信息：{}", attrGroupId);
		AttrGroupEntity attrGroup = attrGroupService.getById(attrGroupId);


        List<Long> catalogIds = categoryService.findcatalogIds(attrGroup.getCatalogId());
        attrGroup.setCatalogIds(catalogIds);

        return R.ok(attrGroup);
    }

    /**
     * 新增属性分组。
     *
     * @param attrGroup 分组内容，主键由数据库生成
     * @return 统一成功响应，不含业务数据
     */
    @RequestMapping("/save")
    public R<Void> save(@RequestBody AttrGroupEntity attrGroup){
        log.info("保存：{}", JSON.toJSONString(attrGroup, SerializerFeature.PrettyFormat));
		attrGroupService.save(attrGroup);

        return R.ok();
    }

    /**
     * 修改属性分组。
     *
     * <p>图标被换掉时先让 third-party 删除旧图标对象，删除失败则整笔回滚。
     *
     * @param attrGroup 分组内容，主键必填
     * @return 统一成功响应，不含业务数据
     */
    @RequestMapping("/update")
    public R<Void> update(@RequestBody AttrGroupEntity attrGroup){
        log.info("修改：{}", JSON.toJSONString(attrGroup, SerializerFeature.PrettyFormat));
		attrGroupService.updateDetail(attrGroup);

        return R.ok();
    }

    /**
     * 按主键批量删除属性分组。
     *
     * <p>分组与属性的关联行、分组图标对象跟着一起删；属性本身不受影响，传不存在的 ID 静默跳过。
     *
     * @param attrGroupIds 待删除的分组主键数组
     * @return 统一成功响应，不含业务数据
     */
    @RequestMapping("/delete")
    public R<Void> delete(@RequestBody Long[] attrGroupIds){
        log.info("删除：{}", JSON.toJSONString(attrGroupIds, SerializerFeature.PrettyFormat));
		attrGroupService.deleteByIds(Arrays.asList(attrGroupIds));

        return R.ok();
    }

}
