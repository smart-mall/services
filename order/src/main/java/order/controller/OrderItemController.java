package order.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import order.entity.OrderItemEntity;
import order.service.OrderItemService;
import common.vo.PageVO;
import common.utils.R;



import common.query.PageQuery;
/**
 * 订单项（{@code oms_order_item} 表）的后台只读接口：分页列表与详情。
 *
 * <p>路径不在 {@code /front} 下，经网关访问时按管理端接口鉴权；订单项由下单链路写入，
 * 后台没有新增、修改与删除 —— 改掉成交金额或数量会与订单头对不上账。
 */
@RestController
@RequestMapping("order/orderitem")
public class OrderItemController {
    @Autowired
    private OrderItemService orderItemService;

    /**
     * 分页查询订单项。
     *
     * @param query 分页参数，{@code page} 为页码、{@code limit} 为每页条数
     * @return 分页结果，{@code rows} 为订单项列表
     */
    @RequestMapping("/list")
    public R<PageVO<OrderItemEntity>> list(PageQuery query){
        PageVO<OrderItemEntity> page = orderItemService.queryPage(query);

        return R.ok(page);
    }


    /**
     * 按主键查询单条订单项。
     *
     * @param id 订单项主键
     * @return 订单项详情；id 不存在时 {@code data} 为 {@code null}
     */
    @RequestMapping("/info/{id}")
    public R<OrderItemEntity> info(@PathVariable("id") Long id){
		OrderItemEntity orderItem = orderItemService.getById(id);

        return R.ok(orderItem);
    }

}
