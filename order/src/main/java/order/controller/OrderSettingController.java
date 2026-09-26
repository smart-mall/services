package order.controller;

import java.util.Arrays;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import order.entity.OrderSettingEntity;
import order.service.OrderSettingService;
import common.vo.PageVO;
import common.utils.R;



import common.query.PageQuery;
/**
 * 订单配置（{@code oms_order_setting} 表）的后台管理接口：分页列表、详情、新增、修改、删除。
 *
 * <p>路径不在 {@code /front} 下，经网关访问时按管理端接口鉴权。
 */
@RestController
@RequestMapping("order/ordersetting")
public class OrderSettingController {
    @Autowired
    private OrderSettingService orderSettingService;

    /**
     * 分页查询订单配置。
     *
     * @param query 分页参数，{@code page} 为页码、{@code limit} 为每页条数
     * @return 分页结果，{@code rows} 为订单配置列表
     */
    @RequestMapping("/list")
    public R<PageVO<OrderSettingEntity>> list(PageQuery query){
        PageVO<OrderSettingEntity> page = orderSettingService.queryPage(query);

        return R.ok(page);
    }


    /**
     * 按主键查询单条订单配置。
     *
     * @param id 订单配置主键
     * @return 订单配置详情；id 不存在时 {@code data} 为 {@code null}
     */
    @RequestMapping("/info/{id}")
    public R<OrderSettingEntity> info(@PathVariable("id") Long id){
		OrderSettingEntity orderSetting = orderSettingService.getById(id);

        return R.ok(orderSetting);
    }

    /**
     * 新增一条订单配置。
     *
     * @param orderSetting 订单配置内容，主键由数据库生成
     * @return 统一成功响应，不含业务数据
     */
    @RequestMapping("/save")
    public R<Void> save(@RequestBody OrderSettingEntity orderSetting){
		orderSettingService.save(orderSetting);

        return R.ok();
    }

    /**
     * 按主键修改一条订单配置。
     *
     * @param orderSetting 订单配置内容，主键必填；为 {@code null} 的字段不参与更新
     * @return 统一成功响应，不含业务数据
     */
    @RequestMapping("/update")
    public R<Void> update(@RequestBody OrderSettingEntity orderSetting){
		orderSettingService.updateById(orderSetting);

        return R.ok();
    }

    /**
     * 按主键批量删除订单配置。
     *
     * @param ids 待删除的订单配置主键数组
     * @return 统一成功响应，不含业务数据
     */
    @RequestMapping("/delete")
    public R<Void> delete(@RequestBody Long[] ids){
		orderSettingService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

}
