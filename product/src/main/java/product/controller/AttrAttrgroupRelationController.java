package product.controller;

import common.vo.PageVO;
import common.utils.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import product.entity.AttrAttrgroupRelationEntity;
import product.service.AttrAttrgroupRelationService;

import java.util.Arrays;


import common.query.PageQuery;
/**
 * 属性与属性分组关联表的后台管理接口：分页列表、详情、新增、修改、删除。
 *
 * <p>路径不在 {@code /front} 下，经网关访问时按管理端接口鉴权；业务侧的批量绑定与解绑走
 * {@link AttrGroupController}。
 */
@RestController
@RequestMapping("product/attrattrgrouprelation")
@Slf4j
public class AttrAttrgroupRelationController {
    @Autowired
    private AttrAttrgroupRelationService attrAttrgroupRelationService;

    /**
     * 分页查询属性与属性分组的关联行。
     *
     * @param query 分页参数，{@code page} 为页码、{@code limit} 为每页条数
     * @return 分页结果，{@code rows} 为关联行列表
     */
    @RequestMapping("/list")
    public R<PageVO<AttrAttrgroupRelationEntity>> list(PageQuery query){
        PageVO<AttrAttrgroupRelationEntity> page = attrAttrgroupRelationService.queryPage(query);

        return R.ok(page);
    }


    /**
     * 按主键查询关联行详情。
     *
     * @param id 关联行主键
     * @return 关联详情；不存在时 {@code data} 为 {@code null}
     */
    @RequestMapping("/info/{id}")
    public R<AttrAttrgroupRelationEntity> info(@PathVariable("id") Long id){
		AttrAttrgroupRelationEntity attrAttrgroupRelation = attrAttrgroupRelationService.getById(id);

        return R.ok(attrAttrgroupRelation);
    }

    /**
     * 新增一条属性与属性分组的关联。
     *
     * @param attrAttrgroupRelation 关联内容，需带 {@code attrId} 与 {@code attrGroupId}
     * @return 统一成功响应，不含业务数据
     */
    @RequestMapping("/save")
    public R<Void> save(@RequestBody AttrAttrgroupRelationEntity attrAttrgroupRelation){
		attrAttrgroupRelationService.save(attrAttrgroupRelation);

        return R.ok();
    }

    /**
     * 按主键修改关联行。
     *
     * @param attrAttrgroupRelation 关联内容，主键必填
     * @return 统一成功响应，不含业务数据
     */
    @RequestMapping("/update")
    public R<Void> update(@RequestBody AttrAttrgroupRelationEntity attrAttrgroupRelation){
		attrAttrgroupRelationService.updateById(attrAttrgroupRelation);

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
		attrAttrgroupRelationService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

}
