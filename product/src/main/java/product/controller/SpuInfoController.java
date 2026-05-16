package product.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import common.utils.PageUtils;
import common.utils.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import product.entity.SpuInfoEntity;
import product.service.SpuInfoService;
import product.vo.SpuSelectVO;
import product.vo.SpuVO;

import java.util.Arrays;
import java.util.List;
import java.util.Map;


/**
 * spu信息
 *
 * @author ??
 * @email sunlightcs@gmail.com
 * @date 2025-09-15 09:58:33
 */
@RestController
@RequestMapping("product/spuinfo")
@Slf4j
public class SpuInfoController {
    private final SpuInfoService spuInfoService;

    public SpuInfoController(SpuInfoService spuInfoService) {
        this.spuInfoService = spuInfoService;
    }

    // 获取spu下拉框选择信息
    @GetMapping(value = "/getSpuSelect")
    public R getSpuSelect() {
        log.info("获取spu下拉框选择信息");
        List<SpuSelectVO> spuSelect = spuInfoService.getSpuSelect();

        return R.ok().setData(spuSelect);
    }

    // 批量获取spuName
    @PostMapping(value = "/getSpuNames")
    public R getSpuNames(@RequestBody List<Long> spuIds) {
        log.info("批量获取spuName：{}", JSON.toJSONString(spuIds, SerializerFeature.PrettyFormat));

        Map<Long, String> map = spuInfoService.getUserNames(spuIds);

        return R.ok().setData(map);
    }

    @GetMapping(value = "/skuId/{skuId}")
    public R getSpuInfoBySkuId(@PathVariable("skuId") Long skuId) {
        log.info("根据skuId查询spu信息");

        SpuInfoEntity spuInfoEntity = spuInfoService.getSpuInfoBySkuId(skuId);

        return R.ok().setData(spuInfoEntity);
    }

    /**
     * 商品上架
     */
    @PostMapping("/{spuId}/up")
    public R up(@PathVariable("spuId") Long spuId) {
        log.info("商品上架：{}", spuId);
        spuInfoService.up(spuId);

        return R.ok();
    }

    /**
     * 列表
     */
    @RequestMapping("/list")
    public R list(@RequestParam Map<String, Object> params){
        log.info("列表查询spu：{}", JSON.toJSONString( params, SerializerFeature.PrettyFormat));
        PageUtils page = spuInfoService.queryPageByCondition(params);

        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    public R info(@PathVariable("id") Long id){
        log.info("根据id查询spu：{}", id);
		SpuInfoEntity spuInfo = spuInfoService.getById(id);

        return R.ok().put("spuInfo", spuInfo);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    public R save(@RequestBody SpuVO spuInfo){
        log.info("保存spu：{}", JSON.toJSONString(spuInfo, SerializerFeature.PrettyFormat));
		spuInfoService.saveSpuInfo(spuInfo);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    public R update(@RequestBody SpuInfoEntity spuInfo){
        log.info("修改spu：{}", JSON.toJSONString(spuInfo, SerializerFeature.PrettyFormat));
		spuInfoService.updateById(spuInfo);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    public R delete(@RequestBody Long[] ids){
		spuInfoService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

}
