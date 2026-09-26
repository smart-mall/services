package ware.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import common.vo.PageVO;
import common.utils.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ware.entity.PurchaseDetailEntity;
import ware.feign.ProductFeignService;
import ware.service.PurchaseDetailService;

import java.util.Arrays;
import java.util.List;
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
    public R<PageVO<PurchaseDetailEntity>> list(@RequestParam Map<String, Object> params){
        log.info("list params:{}", JSON.toJSONString(params, SerializerFeature.PrettyFormat));
        PageVO<PurchaseDetailEntity> page = purchaseDetailService.queryPage(params);

        return R.ok(page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    public R<PurchaseDetailEntity> info(@PathVariable("id") Long id){
        log.info("采购需求单信息: {}", id);
		PurchaseDetailEntity purchaseDetail = purchaseDetailService.getById(id);

        return R.ok(purchaseDetail);
    }

    /**
     * 保存。状态和归属由服务端定，前端传的 status / purchaseId 会被忽略
     */
    @RequestMapping("/save")
    public R<Void> save(@RequestBody PurchaseDetailEntity purchaseDetail){
        log.info("保存采购需求单: {}", JSON.toJSONString(purchaseDetail, SerializerFeature.PrettyFormat));
        purchaseDetailService.saveDetail(purchaseDetail);

        return R.ok();
    }

    /**
     * 修改。只在"新建"状态允许，并入采购单之后要先取消分配
     */
    @RequestMapping("/update")
    public R<Void> update(@RequestBody PurchaseDetailEntity purchaseDetail){
        log.info("修改采购需求单: {}", JSON.toJSONString(purchaseDetail, SerializerFeature.PrettyFormat));
        purchaseDetailService.updateDetail(purchaseDetail);

        return R.ok();
    }

    /**
     * 删除。同样只在"新建"状态允许
     */
    @RequestMapping("/delete")
    public R<Void> delete(@RequestBody Long[] ids){
        log.info("删除采购需求单: {}", JSON.toJSONString(ids, SerializerFeature.PrettyFormat));
        purchaseDetailService.removeDetails(ids == null ? List.of() : Arrays.asList(ids));

        return R.ok();
    }

}
