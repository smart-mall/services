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
 * 订单配置信息
 */
@RestController
@RequestMapping("order/ordersetting")
public class OrderSettingController {
    @Autowired
    private OrderSettingService orderSettingService;

    /**
     * 列表
     */
    @RequestMapping("/list")
    public R<PageVO<OrderSettingEntity>> list(PageQuery query){
        PageVO<OrderSettingEntity> page = orderSettingService.queryPage(query);

        return R.ok(page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    public R<OrderSettingEntity> info(@PathVariable("id") Long id){
		OrderSettingEntity orderSetting = orderSettingService.getById(id);

        return R.ok(orderSetting);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    public R<Void> save(@RequestBody OrderSettingEntity orderSetting){
		orderSettingService.save(orderSetting);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    public R<Void> update(@RequestBody OrderSettingEntity orderSetting){
		orderSettingService.updateById(orderSetting);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    public R<Void> delete(@RequestBody Long[] ids){
		orderSettingService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

}
