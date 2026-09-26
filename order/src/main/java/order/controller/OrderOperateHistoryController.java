package order.controller;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import order.entity.OrderOperateHistoryEntity;
import order.service.OrderOperateHistoryService;
import common.vo.PageVO;
import common.utils.R;



import common.query.PageQuery;
/**
 * 订单操作历史（{@code oms_order_operate_history} 表）的后台管理接口：分页列表、详情、新增、修改、删除。
 *
 * <p>路径不在 {@code /front} 下，经网关访问时按管理端接口鉴权。
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

    /**
     * 新增一条订单操作历史。
     *
     * @param orderOperateHistory 操作历史内容，主键由数据库生成
     * @return 统一成功响应，不含业务数据
     */
    @RequestMapping("/save")
    public R<Void> save(@RequestBody OrderOperateHistoryEntity orderOperateHistory){
		orderOperateHistoryService.save(orderOperateHistory);

        return R.ok();
    }

    /**
     * 按主键修改一条订单操作历史。
     *
     * @param orderOperateHistory 操作历史内容，主键必填；为 {@code null} 的字段不参与更新
     * @return 统一成功响应，不含业务数据
     */
    @RequestMapping("/update")
    public R<Void> update(@RequestBody OrderOperateHistoryEntity orderOperateHistory){
		orderOperateHistoryService.updateById(orderOperateHistory);

        return R.ok();
    }

    /**
     * 按主键批量删除订单操作历史。
     *
     * @param ids 待删除的操作历史主键数组
     * @return 统一成功响应，不含业务数据
     */
    @RequestMapping("/delete")
    public R<Void> delete(@RequestBody Long[] ids){
		orderOperateHistoryService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

}
