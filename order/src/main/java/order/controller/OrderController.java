package order.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import common.vo.PageVO;
import common.utils.R;
import lombok.extern.slf4j.Slf4j;
import order.entity.OrderEntity;
import order.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Map;



import order.vo.OrderStatusVo;
import common.query.PageQuery;
/**
 * 订单（{@code oms_order} 表）的后台管理接口：分页列表、详情、新增、修改、删除。
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
     * @param query 分页参数，{@code page} 为页码、{@code limit} 为每页条数
     * @return 分页结果，{@code rows} 为订单列表
     */
    @RequestMapping("/list")
    public R<PageVO<OrderEntity>> list(PageQuery query){
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
     * 新增一条订单。
     *
     * @param order 订单内容，主键由数据库生成
     * @return 统一成功响应，不含业务数据
     */
    @RequestMapping("/save")
    public R<Void> save(@RequestBody OrderEntity order){
        log.info("保存订单: {}", JSON.toJSONString(order, SerializerFeature.PrettyFormat));
		orderService.save(order);

        return R.ok();
    }

    /**
     * 按主键修改一条订单。
     *
     * @param order 订单内容，主键必填；为 {@code null} 的字段不参与更新
     * @return 统一成功响应，不含业务数据
     */
    @RequestMapping("/update")
    public R<Void> update(@RequestBody OrderEntity order){
        log.info("修改订单: {}", JSON.toJSONString(order, SerializerFeature.PrettyFormat));
		orderService.updateById(order);

        return R.ok();
    }

    /**
     * 按主键批量删除订单。
     *
     * @param ids 待删除的订单主键数组
     * @return 统一成功响应，不含业务数据
     */
    @RequestMapping("/delete")
    public R<Void> delete(@RequestBody Long[] ids){
        log.info("删除订单: {}", JSON.toJSONString(ids, SerializerFeature.PrettyFormat));
		orderService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

}
