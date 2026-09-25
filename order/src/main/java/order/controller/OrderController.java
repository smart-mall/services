package order.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import common.utils.PageUtils;
import common.utils.R;
import lombok.extern.slf4j.Slf4j;
import order.entity.OrderEntity;
import order.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Map;



/**
 * 订单
 *
 * @author ??
 * @email sunlightcs@gmail.com
 * @date 2025-09-15 11:22:13
 */
@RestController
@RequestMapping("order/order")
@Slf4j
public class OrderController {
    @Autowired
    private OrderService orderService;

    /**
     * 列表
     */
    @RequestMapping("/list")
    public R list(@RequestParam Map<String, Object> params){
        log.info("查询订单列表: {}", JSON.toJSONString( params, SerializerFeature.PrettyFormat));
        PageUtils page = orderService.queryPage(params);

        return R.ok().put("page", page);
    }


    /**
     * 查订单状态。
     *
     * <p><b>这是内部接口，不是给 SPA 的</b>：ware 在释放库存前要判断订单是否已取消，
     * 它声明并调用的是 {@code /order/order/status/{orderSn}}
     * （{@code ware/feign/OrderFeignService} → {@code WareSkuServiceImpl#unLockStock}）。
     * 路径正好落在本类的 {@code order/order} 前缀下，所以放在这里，不另开控制器。</p>
     *
     * <p>⚠️ 两件事必须保持：返回的 {@link order.vo.OrderStatusVo} 只带 orderSn/status/statusText
     * —— Feign 从 MQ 监听线程发起、没有请求上下文，这条免登录可访问，返回整单会暴露收货人姓名电话地址；
     * 订单不存在时返回 {@code code=0 + data=null} 而不是报错 —— ware 靠 data==null 判断"必须解锁库存"，
     * 报错会让它抛异常、消息无限重投。</p>
     */
    @GetMapping("/status/{orderSn}")
    public R status(@PathVariable("orderSn") String orderSn){
        return R.ok().setData(orderService.getOrderStatus(orderSn));
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    public R info(@PathVariable("id") Long id){
        log.info("信息: {}", id);
		OrderEntity order = orderService.getById(id);

        return R.ok().put("order", order);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    public R save(@RequestBody OrderEntity order){
        log.info("保存订单: {}", JSON.toJSONString(order, SerializerFeature.PrettyFormat));
		orderService.save(order);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    public R update(@RequestBody OrderEntity order){
        log.info("修改订单: {}", JSON.toJSONString(order, SerializerFeature.PrettyFormat));
		orderService.updateById(order);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    public R delete(@RequestBody Long[] ids){
        log.info("删除订单: {}", JSON.toJSONString(ids, SerializerFeature.PrettyFormat));
		orderService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

}
