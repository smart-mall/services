package ware.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import common.utils.LoginUserUtils;
import common.vo.PageVO;
import common.utils.R;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import ware.entity.PurchaseEntity;
import ware.service.PurchaseService;
import ware.vo.MergeVO;
import ware.vo.PurchaseAssignVO;
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
    public R<Void> done(@RequestBody PurchaseDoneVO purchaseDoneVO){
        log.info("采购单完成: {}", purchaseDoneVO);
        purchaseService.done(purchaseDoneVO);
        return R.ok();
    }

    /**
     * 接受采购单。领取人取网关注入的 {@code X-Admin}，不接受前端传
     */
    @PostMapping("/receive")
    public R<Void> receive(HttpServletRequest request, @RequestBody List<Long> ids){
        log.info("接受采购单: {}", ids);
        purchaseService.receive(LoginUserUtils.requireCurrentAdmin(request).getId(), ids);

        return R.ok();
    }

    /**
     * 合并采购单
     */
    @PostMapping("/merge")
    public R<Void> merge(@RequestBody MergeVO mergeVO){
        log.info("合并采购单: {}", mergeVO);
        purchaseService.merge(mergeVO);

        return R.ok();
    }

    /**
     * 分配采购人员
     */
    @PostMapping("/assign")
    public R<Void> assign(@RequestBody PurchaseAssignVO assignVO){
        log.info("分配采购单: {}", assignVO);
        purchaseService.assign(assignVO);

        return R.ok();
    }

    /**
     * 取消分配：把需求单从采购单里摘出来，退回"新建"
     */
    @PostMapping("/unassign")
    public R<Void> unassign(@RequestBody List<Long> itemIds){
        log.info("取消分配采购需求单: {}", itemIds);
        purchaseService.unassign(itemIds);

        return R.ok();
    }

    /**
     * 列表
     */
    @RequestMapping("/unreceive/list")
    public R<PageVO<PurchaseEntity>> undeceiveList(@RequestParam Map<String, Object> params){
        log.info("未接收的采购单: {}", params);
        PageVO<PurchaseEntity> page = purchaseService.queryPageUnreceive(params);

        return R.ok(page);
    }

    /**
     * 列表
     */
    @RequestMapping("/list")
    public R<PageVO<PurchaseEntity>> list(@RequestParam Map<String, Object> params){
        log.info("采购单列表: {}", JSON.toJSONString(params, SerializerFeature.PrettyFormat));
        PageVO<PurchaseEntity> page = purchaseService.queryPage(params);

        return R.ok(page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    public R<PurchaseEntity> info(@PathVariable("id") Long id){
        log.info("采购单信息: {}", id);
		PurchaseEntity purchase = purchaseService.getById(id);

        return R.ok(purchase);
    }

    /**
     * 删除。只允许删"还没领取、且没有明细"的空单。
     *
     * <p>没有 save / update：采购单由合并需求单时自动生成，唯一的人工写操作是"分配采购人员"。</p>
     */
    @RequestMapping("/delete")
    public R<Void> delete(@RequestBody Long[] ids){
        log.info("删除采购单: {}", JSON.toJSONString(ids, SerializerFeature.PrettyFormat));
		purchaseService.removePurchase(ids == null ? List.of() : Arrays.asList(ids));

        return R.ok();
    }

}
