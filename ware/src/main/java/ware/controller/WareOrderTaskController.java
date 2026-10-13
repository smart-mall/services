package ware.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ware.entity.WareOrderTaskEntity;
import ware.service.WareOrderTaskService;
import common.vo.PageVO;
import common.utils.R;



import common.query.KeyPageQuery;
/**
 * 库存工作单接口：一次订单的库存锁定任务对应一张工作单（订单号、收货人、创建时间），
 * 明细记录锁了哪个 SKU、锁在哪个仓。
 *
 * <p>工作单是库存解锁的入口，解锁时按订单号反查它再逐条处理明细。它是锁定库存时生成的台账，
 * 后台只提供查询，以及"跟着订单一起清掉"的级联删除：手工改 {@code lock_status} 会让同一批库存
 * 被重复解锁，删掉一条已锁定的明细则会让这批库存再也解不开，需要释放库存请走订单侧的关单。
 *
 * <p>明细随列表一起返回，没有独立的明细分页接口。
 */
@RestController
@RequestMapping("ware/wareordertask")
@Slf4j
public class WareOrderTaskController {
    @Autowired
    private WareOrderTaskService wareOrderTaskService;

    /**
     * 分页查询库存工作单，附带每条工作单的明细。
     *
     * @param query 分页与关键字条件；{@code key} 同时匹配订单号与收货人
     * @return 工作单分页数据，每行的 {@code detailList} 已按仓库名填好
     */
    @RequestMapping("/list")
    public R<PageVO<WareOrderTaskEntity>> list(KeyPageQuery query){
        PageVO<WareOrderTaskEntity> page = wareOrderTaskService.queryPage(query);

        return R.ok(page);
    }


    /**
     * 查询库存工作单详情。
     *
     * @param id 工作单 ID
     * @return 工作单；不存在时 {@code data} 为 {@code null}
     */
    @RequestMapping("/info/{id}")
    public R<WareOrderTaskEntity> info(@PathVariable("id") Long id){
		WareOrderTaskEntity wareOrderTask = wareOrderTaskService.getById(id);

        return R.ok(wareOrderTask);
    }

    /**
     * 删除某张订单的库存工作单及其明细。
     *
     * <p>供 order 侧后台删除订单时级联调用：Feign 直连服务，不经网关。工作单仍有"已锁定"的明细时
     * 拒绝删除 —— 这类明细是那批库存唯一的解锁线索，删掉之后库存会永远占着且不报错。</p>
     *
     * @param orderSn 订单号
     * @return 成功响应，无数据；仍有已锁定明细时走兜底码 {@code STOCK_STILL_LOCKED}
     */
    @DeleteMapping("/order/{orderSn}")
    public R<Void> deleteByOrderSn(@PathVariable("orderSn") String orderSn){
        log.info("删除订单的库存工作单: {}", orderSn);
        wareOrderTaskService.deleteByOrderSn(orderSn);

        return R.ok();
    }

}
