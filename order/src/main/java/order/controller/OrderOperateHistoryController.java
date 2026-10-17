package order.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import order.entity.OrderOperateHistoryEntity;
import order.service.OrderOperateHistoryService;
import common.vo.PageVO;
import common.utils.R;



import common.query.PageQuery;
/**
 * 订单操作历史（{@code oms_order_operate_history} 表）的后台只读接口：分页列表与详情。
 *
 * <p>路径不在 {@code /front} 下，经网关访问时按管理端接口鉴权；操作史由订单的各个写入点留痕，
 * 后台没有新增、修改与删除 —— 留痕表能被人手改就不叫留痕了。
 */
@RestController
@RequestMapping("order/orderoperatehistory")
public class OrderOperateHistoryController {
    @Autowired
    private OrderOperateHistoryService orderOperateHistoryService;

    /**
     * 分页查询订单操作历史。
     *
     * @param query 分页参数，{@code page} 为页码、{@code limit} 为每页条数
     * @return 分页结果，{@code rows} 为操作历史列表
     */
    @RequestMapping("/list")
    public R<PageVO<OrderOperateHistoryEntity>> list(PageQuery query){
        PageVO<OrderOperateHistoryEntity> page = orderOperateHistoryService.queryPage(query);

        return R.ok(page);
    }


    /**
     * 按主键查询单条订单操作历史。
     *
     * @param id 操作历史主键
     * @return 操作历史详情；id 不存在时 {@code data} 为 {@code null}
     */
    @RequestMapping("/info/{id}")
    public R<OrderOperateHistoryEntity> info(@PathVariable("id") Long id){
		OrderOperateHistoryEntity orderOperateHistory = orderOperateHistoryService.getById(id);

        return R.ok(orderOperateHistory);
    }

}
