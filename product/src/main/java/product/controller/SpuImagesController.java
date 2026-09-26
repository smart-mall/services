package product.controller;

import common.vo.PageVO;
import common.utils.R;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import product.entity.SpuImagesEntity;
import product.service.SpuImagesService;

import java.util.Arrays;
import java.util.Map;


import common.query.PageQuery;
/**
 * 商品（spu）图集的后台管理接口：分页列表、详情、新增、修改、删除。
 *
 * <p>路径不在 {@code /front} 下，经网关访问时按管理端接口鉴权；新增商品时会连带写入图集，
 * 本接口用于单独维护。
 */
@RestController
@RequestMapping("product/spuimages")
public class SpuImagesController {
    @Autowired
    private SpuImagesService spuImagesService;

    /**
     * 分页查询商品图集。
     *
     * @param query 分页参数，{@code page} 为页码、{@code limit} 为每页条数
     * @return 分页结果，{@code rows} 为图集列表
     */
    @RequestMapping("/list")
    public R<PageVO<SpuImagesEntity>> list(PageQuery query){
        PageVO<SpuImagesEntity> page = spuImagesService.queryPage(query);

        return R.ok(page);
    }


    /**
     * 按主键查询图集记录详情。
     *
     * @param id 图集记录主键
     * @return 图集记录详情；不存在时 {@code data} 为 {@code null}
     */
    @RequestMapping("/info/{id}")
    public R<SpuImagesEntity> info(@PathVariable("id") Long id){
		SpuImagesEntity spuImages = spuImagesService.getById(id);

        return R.ok(spuImages);
    }

    /**
     * 新增一条商品图集记录。
     *
     * @param spuImages 图集内容，需带 {@code spuId} 与图片地址
     * @return 统一成功响应，不含业务数据
     */
    @RequestMapping("/save")
    public R<Void> save(@RequestBody SpuImagesEntity spuImages){
		spuImagesService.save(spuImages);

        return R.ok();
    }

    /**
     * 按主键修改图集记录。
     *
     * @param spuImages 图集内容，主键必填
     * @return 统一成功响应，不含业务数据
     */
    @RequestMapping("/update")
    public R<Void> update(@RequestBody SpuImagesEntity spuImages){
		spuImagesService.updateById(spuImages);

        return R.ok();
    }

    /**
     * 按主键批量删除图集记录。
     *
     * <p>只删数据库记录，不清理 MinIO 里的图片对象。
     *
     * @param ids 待删除的图集记录主键数组
     * @return 统一成功响应，不含业务数据
     */
    @RequestMapping("/delete")
    public R<Void> delete(@RequestBody Long[] ids){
		spuImagesService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

}
