package product.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import common.vo.PageVO;
import common.utils.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import product.entity.SkuInfoEntity;
import product.service.SkuInfoService;
import product.vo.SkuSelectVO;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;


import product.vo.SkuInfoPageQuery;
/**
 * sku信息
 *
 * @author ??
 * @email sunlightcs@gmail.com
 * @date 2025-09-15 09:58:33
 */
@RestController
@RequestMapping("product/skuinfo")
@Slf4j
public class SkuInfoController {
    private final SkuInfoService skuInfoService;

    public SkuInfoController(SkuInfoService skuInfoService) {
        this.skuInfoService = skuInfoService;
    }

    // 获取sku下拉框选择信息
    @GetMapping(value = "/getSkuSelect")
    public R<List<SkuSelectVO>> getSkuSelect() {
        log.info("获取sku下拉框选择信息");
        List<SkuSelectVO> skuSelect = skuInfoService.getSkuSelect();

        return R.ok(skuSelect);
    }


    // 批量获取spuName
    @PostMapping(value = "/getSkuNames")
    public R<Map<Long, String>> getSkuNames(@RequestBody List<Long> spuIds) {
        log.info("批量获取spuName：{}", JSON.toJSONString(spuIds, SerializerFeature.PrettyFormat));

        Map<Long, String> map = skuInfoService.getUserNames(spuIds);

        return R.ok(map);
    }

    /**
     * 根据skuId查询当前商品的价格
     * @param skuId
     * @return
     */
    @GetMapping(value = "/{skuId}/price")
    public BigDecimal getPrice(@PathVariable("skuId") Long skuId) {

        //获取当前商品的信息
        SkuInfoEntity skuInfo = skuInfoService.getById(skuId);

        //获取商品的价格
        BigDecimal price = skuInfo.getPrice();

        return price;
    }

    /**
     * 列表
     */
    @RequestMapping("/list")
    public R<PageVO<SkuInfoEntity>> list(SkuInfoPageQuery query){
        PageVO<SkuInfoEntity> page = skuInfoService.queryPageByCondition(query);

        return R.ok(page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{skuId}")
    public R<SkuInfoEntity> info(@PathVariable("skuId") Long skuId){
		SkuInfoEntity skuInfo = skuInfoService.getById(skuId);

        return R.ok(skuInfo);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    public R<Void> save(@RequestBody SkuInfoEntity skuInfo){
		skuInfoService.save(skuInfo);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    public R<Void> update(@RequestBody SkuInfoEntity skuInfo){
		skuInfoService.updateById(skuInfo);

        return R.ok();
    }

    // 没有 /delete：直接 removeByIds 会绕过 SpuInfoServiceImpl.removeSpuInfo 的整套守卫
    //（不删子表、不查仓库库存与在途采购），等于给"删商品"开了个后门。前端也没有入口调它。
    // sku 的生命周期由 spu 管理，要删就走商品的删除接口。
}
