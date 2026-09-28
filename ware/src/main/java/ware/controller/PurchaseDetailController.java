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



import ware.vo.PurchaseDetailPageQuery;
/**
 * 采购需求单接口：需求单的查询、新建、修改与删除。
 *
 * <p>状态与归属由服务端决定，前端传的 {@code status} / {@code purchaseId} 会被忽略；
 * 状态流转规则见 {@link ware.constants.PurchaseDetailEnum}。
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
     * 分页查询采购需求单。
     *
     * <p>每行的 {@code skuName} 由远程调用商品服务补齐，商品服务不可用时整页查询失败。</p>
     *
     * @param query 分页与筛选条件
     * @return 需求单分页数据，每行带 {@code allowedActions}
     */
    @RequestMapping("/list")
    public R<PageVO<PurchaseDetailEntity>> list(PurchaseDetailPageQuery query){
        log.info("list params:{}", JSON.toJSONString(query, SerializerFeature.PrettyFormat));
        PageVO<PurchaseDetailEntity> page = purchaseDetailService.queryPage(query);

        return R.ok(page);
    }


    /**
     * 查询采购需求单详情。
     *
     * @param id 需求单 ID
     * @return 需求单；不存在时 {@code data} 为 {@code null}
     */
    @RequestMapping("/info/{id}")
    public R<PurchaseDetailEntity> info(@PathVariable("id") Long id){
        log.info("采购需求单信息: {}", id);
		PurchaseDetailEntity purchaseDetail = purchaseDetailService.getById(id);

        return R.ok(purchaseDetail);
    }

    /**
     * 新建采购需求单。
     *
     * <p>状态固定为"新建"、归属清空，并且会远程确认 SKU 仍存在，商品已删则拒绝建单。</p>
     *
     * @param purchaseDetail 需求单内容，{@code skuId}、{@code skuNum}、{@code wareId} 必填
     * @return 成功响应，无数据体
     */
    @RequestMapping("/save")
    public R<Void> save(@RequestBody PurchaseDetailEntity purchaseDetail){
        log.info("保存采购需求单: {}", JSON.toJSONString(purchaseDetail, SerializerFeature.PrettyFormat));
        purchaseDetailService.saveDetail(purchaseDetail);

        return R.ok();
    }

    /**
     * 修改采购需求单，只在"新建"状态允许。
     *
     * <p>并入采购单之后要先取消分配才能改，否则会出现"买 10 件、系统入库 100 件"。</p>
     *
     * @param purchaseDetail 需求单内容，必须带 {@code id}
     * @return 成功响应，无数据体
     */
    @RequestMapping("/update")
    public R<Void> update(@RequestBody PurchaseDetailEntity purchaseDetail){
        log.info("修改采购需求单: {}", JSON.toJSONString(purchaseDetail, SerializerFeature.PrettyFormat));
        purchaseDetailService.updateDetail(purchaseDetail);

        return R.ok();
    }

    /**
     * 删除采购需求单，同样只在"新建"状态允许。
     *
     * @param ids 需求单 ID 数组；为空时服务端按参数校验失败处理
     * @return 成功响应，无数据体
     */
    @RequestMapping("/delete")
    public R<Void> delete(@RequestBody Long[] ids){
        log.info("删除采购需求单: {}", JSON.toJSONString(ids, SerializerFeature.PrettyFormat));
        purchaseDetailService.removeDetails(ids == null ? List.of() : Arrays.asList(ids));

        return R.ok();
    }

}
