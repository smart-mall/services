package order.controller;

import common.query.PageQuery;
import common.utils.R;
import common.vo.PageVO;
import order.entity.RefundInfoEntity;
import order.service.RefundInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 退款信息（{@code oms_refund_info} 表）的后台查询接口。
 *
 * <p><b>只读</b>：这张表是资金台账 —— 一条行代表"有一笔钱确实退出去了"。它只能由售后链路的
 * 退款动作写入（{@code OrderService} 里向平台拿到"资金已变动"的应答之后），
 * 所以这里不给新增、修改与删除：能手工造一条退款流水，就能让账面上出现一笔从没发生过的退款。
 *
 * <p>路径不在 {@code /front} 下，经网关访问时按管理端接口鉴权。
 */
@RestController
@RequestMapping("order/refundinfo")
public class RefundInfoController {

    @Autowired
    private RefundInfoService refundInfoService;

    /**
     * 分页查询退款流水。
     *
     * @param query 分页参数，{@code page} 为页码、{@code limit} 为每页条数
     * @return 分页结果，{@code rows} 为退款流水列表
     */
    @RequestMapping("/list")
    public R<PageVO<RefundInfoEntity>> list(PageQuery query) {
        return R.ok(refundInfoService.queryPage(query));
    }

    /**
     * 按主键查询单条退款流水，含平台返回的原文。
     *
     * @param id 退款流水主键
     * @return 退款流水详情；id 不存在时 {@code data} 为 {@code null}
     */
    @GetMapping("/info/{id}")
    public R<RefundInfoEntity> info(@PathVariable("id") Long id) {
        return R.ok(refundInfoService.getById(id));
    }
}
