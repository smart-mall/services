package order.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import common.vo.PageVO;
import common.utils.R;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import order.entity.OrderEntity;
import order.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;


import order.vo.DeliverVo;
import order.vo.DeliveryCompanyVo;
import order.vo.OrderNoteVo;
import order.vo.OrderPageQuery;
import order.vo.OrderStatusOptionVo;
import order.vo.OrderStatusVo;
/**
 * 订单（{@code oms_order} 表）的后台管理接口：分页列表、详情、发货、关闭、改备注与状态选项查询。
 *
 * <p>不提供按主键改任意字段与按主键删除的接口：订单是财务与售后的凭据，物理删除会留下孤儿订单项，
 * 而自由更新能绕过状态流转；需要作废走 {@link #close(String)}，它带状态前置条件并会释放库存。
 *
 * <p>路径不在 {@code /front} 下，经网关访问时按管理端接口鉴权；另有 ware 释放库存前调用的状态查询，
 * 见 {@link #status(String)}。会员侧接口在 {@code order.web.OrderFrontController}。
 */
@RestController
@RequestMapping("order/order")
@Slf4j
public class OrderController {
    @Autowired
    private OrderService orderService;

    /**
     * 分页查询订单。
     *
     * @param query 查询条件，{@code page} 为页码、{@code limit} 为每页条数；{@code key} 匹配订单号、
     *              收货人与下单会员名，{@code startTime} 与 {@code endTime} 按下单时间过滤，均可为空
     * @return 分页结果，{@code rows} 为订单列表，每行的 {@code statusText} 已填好
     */
    @RequestMapping("/list")
    public R<PageVO<OrderEntity>> list(OrderPageQuery query){
        log.info("查询订单列表: {}", JSON.toJSONString( query, SerializerFeature.PrettyFormat));
        PageVO<OrderEntity> page = orderService.queryPage(query);

        return R.ok(page);
    }


    /**
     * 查询订单状态，供 ware 释放库存前判断订单是否已取消。
     *
     * <p>ware 经 {@code ware/feign/OrderFeignService} 调用 {@code /order/order/status/{orderSn}}，
     * 路径正好落在本类前缀下，所以放在这里而不另开控制器。这条路径免登录，且 Feign 从 MQ 监听线程发起、
     * 没有请求上下文，因此 {@link order.vo.OrderStatusVo} 只带 orderSn、status 与 statusText，
     * 返回整单会暴露收货人姓名与电话；订单不存在时必须返回 {@code code=0 + data=null}，
     * ware 靠 {@code data==null} 判断"必须解锁库存"，抛异常会让消息无限重投。
     *
     * @param orderSn 订单号
     * @return 订单状态；订单不存在时 {@code data} 为 {@code null}
     */
    @GetMapping("/status/{orderSn}")
    public R<OrderStatusVo> status(@PathVariable("orderSn") String orderSn){
        return R.ok(orderService.getOrderStatus(orderSn));
    }


    /**
     * 按主键查询单条订单。
     *
     * @param id 订单主键
     * @return 订单详情；id 不存在时 {@code data} 为 {@code null}
     */
    @RequestMapping("/info/{id}")
    public R<OrderEntity> info(@PathVariable("id") Long id){
        log.info("信息: {}", id);
		OrderEntity order = orderService.getById(id);

        return R.ok(order);
    }

    /**
     * 查询订单详情，附带订单项与状态文案，供后台的详情弹窗使用。
     *
     * <p>与 {@link #info(Long)} 的区别是按业务主键查询并带上订单项：后台看单要知道买了什么，
     * 而订单项不在 {@code oms_order} 表里。</p>
     *
     * @param orderSn 订单号
     * @return 订单详情，{@code orderItemEntityList} 与 {@code statusText} 已填好；
     *         订单不存在时返回 {@code ORDER_NOT_FOUND}
     */
    @GetMapping("/detail/{orderSn}")
    public R<OrderEntity> detail(@PathVariable("orderSn") String orderSn){
        log.info("订单详情: {}", orderSn);

        return R.ok(orderService.getOrderDetailForAdmin(orderSn));
    }

    /**
     * 发货：把已付款的订单置为已发货，写入物流公司与物流单号。
     *
     * <p>只允许已付款的订单发货，重复发货与对未付款订单发货都会返回
     * {@code ORDER_STATUS_INVALID}；物流公司不在支持的清单内时返回参数校验错误。</p>
     *
     * <p>物流单号由服务端生成：项目未对接快递接口，该字段不由调用方传入。</p>
     *
     * <p>出库实扣由 order 落本地消息表后异步通知 ware：本接口返回成功只表示订单状态已流转，
     * <b>不表示库存已经扣完</b>。这是有意的 —— 扣减不会因为缺货失败（可售量在扣减前后不变），
     * 属于可重试的清理动作，异步做才不会出现"货已扣、单还没发货"的跨服务不一致。</p>
     *
     * @param vo 发货参数，含订单号与物流公司，均不能为空
     * @return 统一成功响应，不含业务数据
     */
    @PostMapping("/deliver")
    public R<Void> deliver(@Valid @RequestBody DeliverVo vo){
        log.info("订单发货: {}", JSON.toJSONString(vo, SerializerFeature.PrettyFormat));
        orderService.deliverOrder(vo);

        return R.ok();
    }

    /**
     * 查询支持的物流公司，供发货弹窗的下拉框使用。
     *
     * <p>清单由服务端下发而不是前端写死：写死等于同一份清单存在两处，加一家公司要改两个仓库，
     * 漏掉哪边都不会报错。</p>
     *
     * @return 物流公司选项；{@code code} 用于发货回传，{@code name} 用于展示
     */
    @GetMapping("/delivery-companies")
    public R<List<DeliveryCompanyVo>> deliveryCompanies(){
        return R.ok(orderService.listDeliveryCompanies());
    }

    /**
     * 关闭订单：把待付款的订单置为已取消，并释放库存、退回优惠券。
     *
     * <p>只允许待付款的订单关闭：对已付款或已发货的订单调用返回 {@code ORDER_STATUS_INVALID}，
     * 且不写任何数据。后台作废订单只有这一个入口，直接改状态会漏掉库存释放与退券。</p>
     *
     * @param orderSn 订单号
     * @return 统一成功响应，不含业务数据
     */
    @PostMapping("/close/{orderSn}")
    public R<Void> close(@PathVariable("orderSn") String orderSn){
        log.info("关闭订单: {}", orderSn);
        orderService.closeOrderByAdmin(orderSn);

        return R.ok();
    }

    /**
     * 修改订单备注。
     *
     * @param vo 备注参数，{@code orderSn} 不能为空，{@code note} 传空表示清空备注
     * @return 统一成功响应，不含业务数据
     */
    @PostMapping("/note")
    public R<Void> note(@Valid @RequestBody OrderNoteVo vo){
        log.info("修改订单备注: {}", JSON.toJSONString(vo, SerializerFeature.PrettyFormat));
        orderService.updateNote(vo.getOrderSn(), vo.getNote());

        return R.ok();
    }

    /**
     * 查询订单状态选项，供列表页状态筛选的下拉框使用。
     *
     * <p>选项由服务端下发：状态文案只在 {@link order.enums.OrderStatusEnum} 里维护一份，
     * 前端再抄一份就多了一个会漂移的副本。</p>
     *
     * @return 状态选项；{@code code} 用于筛选回传，{@code text} 用于展示
     */
    @GetMapping("/status-options")
    public R<List<OrderStatusOptionVo>> statusOptions(){
        return R.ok(orderService.listStatusOptions());
    }

}
