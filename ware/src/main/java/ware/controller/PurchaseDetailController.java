package ware.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import common.utils.PageUtils;
import common.utils.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ware.entity.PurchaseDetailEntity;
import ware.feign.ProductFeignService;
import ware.service.PurchaseDetailService;

import java.util.Arrays;
import java.util.Map;



/**
 * 
 *
 * @author ??
 * @email sunlightcs@gmail.com
 * @date 2025-09-15 11:20:17
 */
@RestController
@RequestMapping("ware/purchasedetail")
@Slf4j
public class PurchaseDetailController {
    private final PurchaseDetailService purchaseDetailService;
    private final ProductFeignService productFeignService;

    public PurchaseDetailController(PurchaseDetailService purchaseDetailService, ProductFeignService productFeignService) {
        this.purchaseDetailService = purchaseDetailService;
        this.productFeignService = productFeignService;
    }

    /**
     * 列表
     */
    @RequestMapping("/list")
    public R list(@RequestParam Map<String, Object> params){
        log.info("list params:{}", JSON.toJSONString(params, SerializerFeature.PrettyFormat));
        PageUtils page = purchaseDetailService.queryPage(params);

        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    public R info(@PathVariable("id") Long id){
        log.info("采购单信息: {}", id);
		PurchaseDetailEntity purchaseDetail = purchaseDetailService.getById(id);

        return R.ok().put("purchaseDetail", purchaseDetail);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    public R save(@RequestBody PurchaseDetailEntity purchaseDetail){
        log.info("保存采购单: {}", JSON.toJSONString(purchaseDetail, SerializerFeature.PrettyFormat));
        purchaseDetailService.save(purchaseDetail);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    public R update(@RequestBody PurchaseDetailEntity purchaseDetail){
        log.info("修改采购单: {}", JSON.toJSONString(purchaseDetail, SerializerFeature.PrettyFormat));
		purchaseDetailService.updateById(purchaseDetail);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    public R delete(@RequestBody Long[] ids){
        log.info("删除采购单: {}", JSON.toJSONString(ids, SerializerFeature.PrettyFormat));
		purchaseDetailService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

}
