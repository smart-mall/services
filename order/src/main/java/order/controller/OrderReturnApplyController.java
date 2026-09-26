package order.controller;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import order.entity.OrderReturnApplyEntity;
import order.service.OrderReturnApplyService;
import common.vo.PageVO;
import common.utils.R;



import common.query.PageQuery;
/**
 * 退货申请单（{@code oms_order_return_apply} 表）的后台管理接口：分页列表、详情、新增、修改、删除。
 *
 * <p>路径不在 {@code /front} 下，经网关访问时按管理端接口鉴权。
 */
@RestController
@RequestMapping("order/orderreturnapply")
public class OrderReturnApplyController {
    @Autowired
    private OrderReturnApplyService orderReturnApplyService;

    /**
     * 分页查询退货申请单。
     *
     * @param query 分页参数，{@code page} 为页码、{@code limit} 为每页条数
     * @return 分页结果，{@code rows} 为退货申请单列表
     */
    @RequestMapping("/list")
    public R<PageVO<OrderReturnApplyEntity>> list(PageQuery query){
        PageVO<OrderReturnApplyEntity> page = orderReturnApplyService.queryPage(query);

        return R.ok(page);
    }


    /**
     * 按主键查询单条退货申请单。
     *
     * @param id 退货申请单主键
     * @return 退货申请单详情；id 不存在时 {@code data} 为 {@code null}
     */
    @RequestMapping("/info/{id}")
    public R<OrderReturnApplyEntity> info(@PathVariable("id") Long id){
		OrderReturnApplyEntity orderReturnApply = orderReturnApplyService.getById(id);

        return R.ok(orderReturnApply);
    }

    /**
     * 新增一条退货申请单。
     *
     * @param orderReturnApply 退货申请单内容，主键由数据库生成
     * @return 统一成功响应，不含业务数据
     */
    @RequestMapping("/save")
    public R<Void> save(@RequestBody OrderReturnApplyEntity orderReturnApply){
		orderReturnApplyService.save(orderReturnApply);

        return R.ok();
    }

    /**
     * 按主键修改一条退货申请单。
     *
     * @param orderReturnApply 退货申请单内容，主键必填；为 {@code null} 的字段不参与更新
     * @return 统一成功响应，不含业务数据
     */
    @RequestMapping("/update")
    public R<Void> update(@RequestBody OrderReturnApplyEntity orderReturnApply){
		orderReturnApplyService.updateById(orderReturnApply);

        return R.ok();
    }

    /**
     * 按主键批量删除退货申请单。
     *
     * @param ids 待删除的退货申请单主键数组
     * @return 统一成功响应，不含业务数据
     */
    @RequestMapping("/delete")
    public R<Void> delete(@RequestBody Long[] ids){
		orderReturnApplyService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

}
