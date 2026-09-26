package product.controller;

import common.vo.PageVO;
import common.utils.R;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import product.entity.SkuImagesEntity;
import product.service.SkuImagesService;

import java.util.Arrays;


import common.query.PageQuery;
/**
 * sku 图片的后台管理接口：分页列表、详情、新增、修改、删除。
 *
 * <p>路径不在 {@code /front} 下，经网关访问时按管理端接口鉴权；新增商品时会连带写入 sku 图片，
 * 本接口用于单独维护。
 */
@RestController
@RequestMapping("product/skuimages")
public class SkuImagesController {
    @Autowired
    private SkuImagesService skuImagesService;

    /**
     * 分页查询 sku 图片。
     *
     * @param query 分页参数，{@code page} 为页码、{@code limit} 为每页条数
     * @return 分页结果，{@code rows} 为 sku 图片列表
     */
    @RequestMapping("/list")
    public R<PageVO<SkuImagesEntity>> list(PageQuery query){
        PageVO<SkuImagesEntity> page = skuImagesService.queryPage(query);

        return R.ok(page);
    }


    /**
     * 按主键查询 sku 图片详情。
     *
     * @param id sku 图片主键
     * @return sku 图片详情；不存在时 {@code data} 为 {@code null}
     */
    @RequestMapping("/info/{id}")
    public R<SkuImagesEntity> info(@PathVariable("id") Long id){
		SkuImagesEntity skuImages = skuImagesService.getById(id);

        return R.ok(skuImages);
    }

    /**
     * 新增一条 sku 图片。
     *
     * @param skuImages sku 图片内容，需带 {@code skuId} 与图片地址
     * @return 统一成功响应，不含业务数据
     */
    @RequestMapping("/save")
    public R<Void> save(@RequestBody SkuImagesEntity skuImages){
		skuImagesService.save(skuImages);

        return R.ok();
    }

    /**
     * 按主键修改 sku 图片。
     *
     * @param skuImages sku 图片内容，主键必填
     * @return 统一成功响应，不含业务数据
     */
    @RequestMapping("/update")
    public R<Void> update(@RequestBody SkuImagesEntity skuImages){
		skuImagesService.updateById(skuImages);

        return R.ok();
    }

    /**
     * 按主键批量删除 sku 图片。
     *
     * <p>只删数据库记录，不清理 MinIO 里的图片对象。
     *
     * @param ids 待删除的 sku 图片主键数组
     * @return 统一成功响应，不含业务数据
     */
    @RequestMapping("/delete")
    public R<Void> delete(@RequestBody Long[] ids){
		skuImagesService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

}
