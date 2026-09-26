package ware.controller;

import common.exception.NoStockException;
import common.vo.PageVO;
import common.to.SkuDeleteBlockerTo;
import common.utils.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import ware.entity.WareSkuEntity;
import ware.service.WareSkuService;
import ware.vo.SkuHasStockVo;
import ware.vo.WareSkuLockVo;

import java.util.List;

import ware.vo.WareSkuPageQuery;
import static common.exception.BaseCodeEnum.NO_STOCK_EXCEPTION;


/**
 * 商品库存接口：库存锁定、可售库存查询，以及商品删除前的仓库侧守卫。
 *
 * <p>没有 save / update / delete：库存行只由"采购完成"创建、{@code stock} 只由采购增加、
 * {@code stock_locked} 只由订单增减，没有一条合法路径需要人工写；唯一会删库存行的是商品删除后
 * 那条 MQ 消息（只清零行），走监听器不走这里。
 */
@RestController
@RequestMapping("ware/waresku")
@Slf4j
public class WareSkuController {
    @Autowired
    private WareSkuService wareSkuService;

    /**
     * 锁定订单占用的库存。
     *
     * <p>逐个 SKU 找到有货的仓库并扣减可售量，全部锁定成功才返回成功；任一 SKU 在所有仓库都锁不上
     * 就抛 {@link NoStockException} 并回滚，这里捕获后转成统一的无库存错误码，不向外抛异常。</p>
     *
     * @param vo 订单号与需要锁定的 SKU 明细，不能为 {@code null}
     * @return 锁定结果；库存不足时 {@code code} 为无库存错误码，{@code data} 为空
     */
    @PostMapping(value = "/lock/order")
    public R<Boolean> orderLockStock(@RequestBody WareSkuLockVo vo) {
        log.info("锁定库存");

        try {
            boolean lockStock = wareSkuService.orderLockStock(vo);
            return R.ok(lockStock);
        } catch (NoStockException e) {
            return R.error(NO_STOCK_EXCEPTION.getCode(),NO_STOCK_EXCEPTION.getMsg());
        }
    }

    /**
     * 查询这些 SKU 是否有可售库存。
     *
     * @param skuIds 商品 SKU ID 列表
     * @return 每个入参 SKU 一条结果，顺序与入参一致；没有库存的 SKU 也会返回，{@code hasStock} 为 {@code false}
     */
    @PostMapping(value = "/hasStock")
    public R<List<SkuHasStockVo>> getSkuHasStock(@RequestBody List<Long> skuIds) {
        log.info("判断是否有库存：{}", skuIds);

        //skuId stock
        List<SkuHasStockVo> vos = wareSkuService.getSkusHasStock(skuIds);

        return R.ok(vos);

    }

    /**
     * 批量查询这些 SKU 是否有可售库存。
     *
     * <p>语义与 {@link #getSkuHasStock(List)} 相同，供 {@code /hasstock} 路径调用。</p>
     *
     * @param skuIds 商品 SKU ID 列表
     * @return 每个入参 SKU 一条结果，顺序与入参一致
     */
    @PostMapping("/hasstock")
    public R<List<SkuHasStockVo>> getSkusHasStock(@RequestBody List<Long> skuIds) {
        List<SkuHasStockVo> vos = wareSkuService.getSkusHasStock(skuIds);
        return R.ok(vos);
    }

    /**
     * 判断这些 SKU 在仓库侧还能不能删，供 product 删除商品前做守卫。
     *
     * <p>返回的 data 是阻塞清单：空集合表示都能删；非空时每一项说明这个 sku
     * 在哪个仓还有多少件、还有几条没走完的采购需求。文案由调用方拼。</p>
     *
     * @param skuIds 待删除的商品 SKU ID 列表
     * @return 每个有阻塞的 SKU 一条记录；全部可删时为空集合
     */
    @PostMapping("/canDelete")
    public R<List<SkuDeleteBlockerTo>> canDelete(@RequestBody List<Long> skuIds) {
        log.info("判断商品能否从仓库删除：{}", skuIds);
        return R.ok(wareSkuService.canDelete(skuIds));
    }

    /**
     * 分页查询商品库存。
     *
     * @param query 分页与筛选条件
     * @return 库存分页数据
     */
    @RequestMapping("/list")
    public R<PageVO<WareSkuEntity>> list(WareSkuPageQuery query){
        PageVO<WareSkuEntity> page = wareSkuService.queryPage(query);

        return R.ok(page);
    }


    /**
     * 查询单条商品库存。
     *
     * @param id 库存行 ID
     * @return 库存行；不存在时 {@code data} 为 {@code null}
     */
    @RequestMapping("/info/{id}")
    public R<WareSkuEntity> info(@PathVariable("id") Long id){
		WareSkuEntity wareSku = wareSkuService.getById(id);

        return R.ok(wareSku);
    }

}
