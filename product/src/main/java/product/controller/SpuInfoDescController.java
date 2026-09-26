package product.controller;

import common.vo.PageVO;
import common.utils.R;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import product.entity.SpuInfoDescEntity;
import product.service.SpuInfoDescService;

import java.util.Arrays;
import java.util.Map;


import common.query.PageQuery;
/**
 * 商品介绍的后台管理接口：分页列表、详情、新增、修改、删除。
 *
 * <p>路径不在 {@code /front} 下，经网关访问时按管理端接口鉴权；一个 spu 只有一条介绍，因此详情
 * 与删除都按 {@code spuId} 定位。
 */
@RestController
@RequestMapping("product/spuinfodesc")
public class SpuInfoDescController {
    @Autowired
    private SpuInfoDescService spuInfoDescService;

    /**
     * 分页查询商品介绍。
     *
     * @param query 分页参数，{@code page} 为页码、{@code limit} 为每页条数
     * @return 分页结果，{@code rows} 为介绍列表
     */
    @RequestMapping("/list")
    public R<PageVO<SpuInfoDescEntity>> list(PageQuery query){
        PageVO<SpuInfoDescEntity> page = spuInfoDescService.queryPage(query);

        return R.ok(page);
    }


    /**
     * 按 spu 主键查询商品介绍。
     *
     * @param spuId spu ID
     * @return 商品介绍；不存在时 {@code data} 为 {@code null}
     */
    @RequestMapping("/info/{spuId}")
    public R<SpuInfoDescEntity> info(@PathVariable("spuId") Long spuId){
		SpuInfoDescEntity spuInfoDesc = spuInfoDescService.getById(spuId);

        return R.ok(spuInfoDesc);
    }

    /**
     * 新增一条商品介绍。
     *
     * @param spuInfoDesc 介绍内容，{@code spuId} 必填，与 spu 一一对应
     * @return 统一成功响应，不含业务数据
     */
    @RequestMapping("/save")
    public R<Void> save(@RequestBody SpuInfoDescEntity spuInfoDesc){
		spuInfoDescService.save(spuInfoDesc);

        return R.ok();
    }

    /**
     * 按主键修改商品介绍。
     *
     * @param spuInfoDesc 介绍内容，{@code spuId} 必填
     * @return 统一成功响应，不含业务数据
     */
    @RequestMapping("/update")
    public R<Void> update(@RequestBody SpuInfoDescEntity spuInfoDesc){
		spuInfoDescService.updateById(spuInfoDesc);

        return R.ok();
    }

    /**
     * 按 spu 主键批量删除商品介绍。
     *
     * @param spuIds 待删除的 spu 主键数组
     * @return 统一成功响应，不含业务数据
     */
    @RequestMapping("/delete")
    public R<Void> delete(@RequestBody Long[] spuIds){
		spuInfoDescService.removeByIds(Arrays.asList(spuIds));

        return R.ok();
    }

}
