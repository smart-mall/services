package product.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import common.vo.PageVO;
import common.utils.R;
import common.valid.AddGroup;
import common.valid.UpdateGroup;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import product.entity.BrandEntity;
import product.service.BrandService;

import java.util.Arrays;
import java.util.Map;


/**
 * 品牌
 *
 * @author ??
 * @email sunlightcs@gmail.com
 * @date 2025-09-15 09:58:33
 */
@RestController
@RequestMapping("product/brand")
@Slf4j
public class BrandController {
    @Autowired
    private BrandService brandService;

    /**
     * 列表
     */
    @RequestMapping("/list")
    public R<PageVO<BrandEntity>> list(@RequestParam Map<String, Object> params){
        log.info("显示品牌：{}", JSON.toJSONString( params, SerializerFeature.PrettyFormat));
        PageVO<BrandEntity> page = brandService.queryPage(params);

        return R.ok(page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{brandId}")
    public R<BrandEntity> info(@PathVariable("brandId") Long brandId){
        log.info("获取品牌：{}", brandId);
		BrandEntity brand = brandService.getById(brandId);

        return R.ok(brand);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    public R<Void> save(@Validated(AddGroup.class) @RequestBody BrandEntity brand){
        log.info("保存品牌：{}", JSON.toJSONString(brand, SerializerFeature.PrettyFormat));

		brandService.save(brand);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    public R<Void> update(@Validated(UpdateGroup.class) @RequestBody BrandEntity brand){
        log.info("修改品牌：{}", JSON.toJSONString(brand, SerializerFeature.PrettyFormat));
		brandService.updateDetail(brand);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    public R<Void> delete(@RequestBody Long[] brandIds){
        log.info("删除品牌：{}", JSON.toJSONString(brandIds, SerializerFeature.PrettyFormat));
		brandService.deleteByIds(Arrays.asList(brandIds));

        return R.ok();
    }

}
