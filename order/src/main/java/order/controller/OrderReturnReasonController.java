package order.controller;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import order.entity.OrderReturnReasonEntity;
import order.service.OrderReturnReasonService;
import common.vo.PageVO;
import common.utils.R;



import common.query.PageQuery;
/**
 * 退货原因（{@code oms_order_return_reason} 表）的后台管理接口：分页列表、详情、新增、修改、删除。
 *
 * <p>路径不在 {@code /front} 下，经网关访问时按管理端接口鉴权。
 */
@RestController
@RequestMapping("order/orderreturnreason")
public class OrderReturnReasonController {
    @Autowired
    private OrderReturnReasonService orderReturnReasonService;

    /**
     * 分页查询退货原因。
     *
     * @param query 分页参数，{@code page} 为页码、{@code limit} 为每页条数
     * @return 分页结果，{@code rows} 为退货原因列表
     */
    @RequestMapping("/list")
    public R<PageVO<OrderReturnReasonEntity>> list(PageQuery query){
        PageVO<OrderReturnReasonEntity> page = orderReturnReasonService.queryPage(query);

        return R.ok(page);
    }


    /**
     * 按主键查询单条退货原因。
     *
     * @param id 退货原因主键
     * @return 退货原因详情；id 不存在时 {@code data} 为 {@code null}
     */
    @RequestMapping("/info/{id}")
    public R<OrderReturnReasonEntity> info(@PathVariable("id") Long id){
		OrderReturnReasonEntity orderReturnReason = orderReturnReasonService.getById(id);

        return R.ok(orderReturnReason);
    }

    /**
     * 新增一条退货原因。
     *
     * @param orderReturnReason 退货原因内容，主键由数据库生成
     * @return 统一成功响应，不含业务数据
     */
    @RequestMapping("/save")
    public R<Void> save(@RequestBody OrderReturnReasonEntity orderReturnReason){
		orderReturnReasonService.save(orderReturnReason);

        return R.ok();
    }

    /**
     * 按主键修改一条退货原因。
     *
     * @param orderReturnReason 退货原因内容，主键必填；为 {@code null} 的字段不参与更新
     * @return 统一成功响应，不含业务数据
     */
    @RequestMapping("/update")
    public R<Void> update(@RequestBody OrderReturnReasonEntity orderReturnReason){
		orderReturnReasonService.updateById(orderReturnReason);

        return R.ok();
    }

    /**
     * 按主键批量删除退货原因。
     *
     * @param ids 待删除的退货原因主键数组
     * @return 统一成功响应，不含业务数据
     */
    @RequestMapping("/delete")
    public R<Void> delete(@RequestBody Long[] ids){
		orderReturnReasonService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

}
