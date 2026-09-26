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
 * spu图片
 */
@RestController
@RequestMapping("product/spuimages")
public class SpuImagesController {
    @Autowired
    private SpuImagesService spuImagesService;

    /**
     * 列表
     */
    @RequestMapping("/list")
    public R<PageVO<SpuImagesEntity>> list(PageQuery query){
        PageVO<SpuImagesEntity> page = spuImagesService.queryPage(query);

        return R.ok(page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    public R<SpuImagesEntity> info(@PathVariable("id") Long id){
		SpuImagesEntity spuImages = spuImagesService.getById(id);

        return R.ok(spuImages);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    public R<Void> save(@RequestBody SpuImagesEntity spuImages){
		spuImagesService.save(spuImages);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    public R<Void> update(@RequestBody SpuImagesEntity spuImages){
		spuImagesService.updateById(spuImages);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    public R<Void> delete(@RequestBody Long[] ids){
		spuImagesService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

}
