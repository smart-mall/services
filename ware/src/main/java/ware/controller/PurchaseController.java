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



import common.query.PageQuery;
import ware.vo.PurchasePageQuery;
/**
 * 采购单接口：采购单的查询、合并需求单、分配与领取采购员、提交完成与删除。
 *
 * <p>采购单由合并采购需求单时自动生成，状态流转规则见 {@link ware.constants.PurchaseStatusEnum}，
 * 本类只做参数接收与转发，校验都在 service 层。
 */
@RestController
@RequestMapping("ware/purchase")
@Slf4j
public class PurchaseController {
    @Autowired
    private PurchaseService purchaseService;


    /**
     * 提交采购结果，完成采购单。
     *
     * <p>每条明细只接受"已完成"或"采购失败"两种结果，全部成功时采购单落到已完成，
     * 否则落到有异常。</p>
     *
     * @param purchaseDoneVO 采购单 ID 与逐条明细的采购结果，不能为 {@code null}
     * @return 成功响应，无数据体
     */
    @PostMapping("/done")
    public R<Void> done(@RequestBody PurchaseDoneVO purchaseDoneVO){
        log.info("采购单完成: {}", purchaseDoneVO);
        purchaseService.done(purchaseDoneVO);
        return R.ok();
    }

    /**
     * 领取采购单。
     *
     * <p>领取人取网关注入的 {@code X-Admin}，不接受前端传，否则可以冒领别人分配的单。</p>
     *
     * @param request 当前请求，管理员身份从 {@code X-Admin} 头解析
     * @param ids 要领取的采购单 ID 列表，不能为空
     * @return 成功响应，无数据体
     */
    @PostMapping("/receive")
    public R<Void> receive(HttpServletRequest request, @RequestBody List<Long> ids){
        log.info("接受采购单: {}", ids);
        purchaseService.receive(LoginUserUtils.requireCurrentAdmin(request).getId(), ids);

        return R.ok();
    }

    /**
     * 合并采购需求单：未指定采购单时新建一张，指定时并入已有单。
     *
     * @param mergeVO 需求单 ID 列表与目标采购单 ID，不能为 {@code null}
     * @return 成功响应，无数据体
     */
    @PostMapping("/merge")
    public R<Void> merge(@RequestBody MergeVO mergeVO){
        log.info("合并采购单: {}", mergeVO);
        purchaseService.merge(mergeVO);

        return R.ok();
    }

    /**
     * 给采购单分配采购人员。
     *
     * @param assignVO 采购单 ID 与采购员信息，不能为 {@code null}
     * @return 成功响应，无数据体
     */
    @PostMapping("/assign")
    public R<Void> assign(@RequestBody PurchaseAssignVO assignVO){
        log.info("分配采购单: {}", assignVO);
        purchaseService.assign(assignVO);

        return R.ok();
    }

    /**
     * 取消分配：把需求单从采购单里摘出来，退回"新建"。
     *
     * @param itemIds 采购需求单 ID 列表，不能为空
     * @return 成功响应，无数据体
     */
    @PostMapping("/unassign")
    public R<Void> unassign(@RequestBody List<Long> itemIds){
        log.info("取消分配采购需求单: {}", itemIds);
        purchaseService.unassign(itemIds);

        return R.ok();
    }

    /**
     * 分页查询还没被领取的采购单。
     *
     * @param query 分页参数
     * @return 采购单分页数据
     */
    @RequestMapping("/unreceive/list")
    public R<PageVO<PurchaseEntity>> undeceiveList(PageQuery query){
        log.info("未接收的采购单: {}", query);
        PageVO<PurchaseEntity> page = purchaseService.queryPageUnreceive(query);

        return R.ok(page);
    }

    /**
     * 分页查询采购单。
     *
     * @param query 分页与筛选条件
     * @return 采购单分页数据，每行带 {@code allowedActions}
     */
    @RequestMapping("/list")
    public R<PageVO<PurchaseEntity>> list(PurchasePageQuery query){
        log.info("采购单列表: {}", JSON.toJSONString(query, SerializerFeature.PrettyFormat));
        PageVO<PurchaseEntity> page = purchaseService.queryPage(query);

        return R.ok(page);
    }


    /**
     * 查询采购单详情。
     *
     * @param id 采购单 ID
     * @return 采购单；不存在时 {@code data} 为 {@code null}
     */
    @RequestMapping("/info/{id}")
    public R<PurchaseEntity> info(@PathVariable("id") Long id){
        log.info("采购单信息: {}", id);
		PurchaseEntity purchase = purchaseService.getById(id);

        return R.ok(purchase);
    }

    /**
     * 删除采购单，只允许删还没领取、且没有明细的空单。
     *
     * <p>没有 save / update：采购单由合并需求单时自动生成，唯一的人工写操作是分配采购人员。</p>
     *
     * @param ids 采购单 ID 数组；为空时服务端按参数校验失败处理
     * @return 成功响应，无数据体
     */
    @RequestMapping("/delete")
    public R<Void> delete(@RequestBody Long[] ids){
        log.info("删除采购单: {}", JSON.toJSONString(ids, SerializerFeature.PrettyFormat));
		purchaseService.removePurchase(ids == null ? List.of() : Arrays.asList(ids));

        return R.ok();
    }

}
