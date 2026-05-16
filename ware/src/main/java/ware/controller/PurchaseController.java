package ware.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import common.utils.PageUtils;
import common.utils.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import ware.entity.PurchaseEntity;
import ware.service.PurchaseService;
import ware.vo.MergeVO;
import ware.vo.PurchaseDoneVO;

import java.util.Arrays;
import java.util.List;
import java.util.Map;



/**
 * 采购信息
 *
 * @author ??
 * @email sunlightcs@gmail.com
 * @date 2025-09-15 11:20:17
 */
@RestController
@RequestMapping("ware/purchase")
@Slf4j
public class PurchaseController {
    @Autowired
    private PurchaseService purchaseService;


    /**
     * 采购单完成
     */
    @PostMapping("/done")
    public R done(@RequestBody PurchaseDoneVO purchaseDoneVO){
        log.info("采购单完成: {}", purchaseDoneVO);
        purchaseService.done(purchaseDoneVO);
        return R.ok();
    }
    /**
     * 接受采购单
     */
    @PostMapping("/receive")
    public R receive(@RequestBody List<Long> ids){
        log.info("接受采购单: {}", ids);
        purchaseService.receive(ids);

        return R.ok();
    }


    /**
     * 合并采购单
     */
    @PostMapping("/merge")
    public R merge(@RequestBody MergeVO mergeVO){
        log.info("合并采购单: {}", mergeVO);
        purchaseService.merge(mergeVO);

        return R.ok();
    }

    /**
     * 列表
     */
    @RequestMapping("/unreceive/list")
    public R undeceiveList(@RequestParam Map<String, Object> params){
        log.info("未接收的采购单: {}", params);
        PageUtils page = purchaseService.queryPageUnreceive(params);

        return R.ok().put("page", page);
    }

    /**
     * 列表
     */
    @RequestMapping("/list")
    public R list(@RequestParam Map<String, Object> params){
        log.info("采购单列表: {}", JSON.toJSONString(params, SerializerFeature.PrettyFormat));
        PageUtils page = purchaseService.queryPage(params);

        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    public R info(@PathVariable("id") Long id){
        log.info("采购单信息: {}", id);
		PurchaseEntity purchase = purchaseService.getById(id);

        return R.ok().put("purchase", purchase);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    public R save(@RequestBody PurchaseEntity purchase){
        log.info("保存采购单: {}", purchase);
		purchaseService.save(purchase);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    public R update(@RequestBody PurchaseEntity purchase){
        log.info("修改采购单: {}", purchase);
		purchaseService.updateById(purchase);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    public R delete(@RequestBody Long[] ids){
        log.info("删除采购单: {}", JSON.toJSONString(ids, SerializerFeature.PrettyFormat));
		purchaseService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

}
