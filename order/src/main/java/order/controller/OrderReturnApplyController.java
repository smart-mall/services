package order.controller;

import common.utils.LoginUserUtils;
import common.utils.R;
import common.vo.PageVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import order.entity.OrderReturnApplyEntity;
import order.service.OrderService;
import order.vo.AfterSaleAuditVo;
import order.vo.AfterSalePageQuery;
import order.vo.AfterSaleReceiveVo;
import order.vo.OrderStatusOptionVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 售后申请（{@code oms_order_return_apply} 表）的后台接口。
 *
 * <p>这张表装的是两条链的申请单：<b>仅退款</b>（已付款未发货，整单退钱）与<b>退货退款</b>
 * （确认收货之后，按订单项寄回）。两者共用一张表、靠 {@code type} 分开，
 * 所以后台看的是同一个台账、按类型筛出两个页面。
 *
 * <p><b>刻意不给"按主键改任意字段"的接口</b>：售后单上的状态、退款金额、处理人都是有业务含义的，
 * 自由更新能绕过整条流程（手工把状态改成"已完成"就等于凭空退了一笔钱）。可写的动作只有三个 ——
 * 申请（会员端）、审核、确认收到退货，全部走 service 里带状态条件的流转。
 *
 * <p>路径不在 {@code /front} 下，经网关访问时按管理端接口鉴权；操作人一律取网关注入的
 * {@code X-Admin}，不由前端传。
 */
@RestController
@RequestMapping("order/orderreturnapply")
public class OrderReturnApplyController {

    @Autowired
    private OrderService orderService;

    /**
     * 分页查询售后申请单，供后台的退货台账与退款台账共用。
     *
     * <p>{@code type} 区分两条链（不传则两种都返回），{@code status} 收窄处理进度，
     * {@code key} 匹配订单号与申请会员名，时间区间按申请时间比较。每行的类型与状态文案由服务端填好。</p>
     *
     * @param query 查询条件，除分页外的字段都为空时不过滤
     * @return 分页结果，{@code rows} 为售后申请单列表
     */
    @RequestMapping("/list")
    public R<PageVO<OrderReturnApplyEntity>> list(AfterSalePageQuery query) {
        return R.ok(orderService.queryAfterSalePage(query));
    }

    /**
     * 按主键查询单条售后申请单，供详情弹窗使用。
     *
     * <p>只读。返回实体而不是会员端用的投影：后台要看得到处理人（谁批的、谁收的货）。</p>
     *
     * @param id 售后申请单主键
     * @return 售后申请单详情；id 不存在时 {@code data} 为 {@code null}
     */
    @GetMapping("/info/{id}")
    public R<OrderReturnApplyEntity> info(@PathVariable("id") Long id) {
        return R.ok(orderService.getOrderReturnApply(id));
    }

    /**
     * 查询售后申请的状态选项，供台账状态筛选的下拉框使用。
     *
     * <p>选项由服务端下发：状态文案只在 {@link order.enums.AfterSaleStatusEnum} 里维护一份。</p>
     *
     * @return 状态选项；{@code code} 用于筛选回传，{@code text} 用于展示
     */
    @GetMapping("/status-options")
    public R<List<OrderStatusOptionVo>> statusOptions() {
        return R.ok(orderService.listAfterSaleStatusOptions());
    }

    /**
     * 审核售后申请：通过或拒绝。
     *
     * <p>通过的含义按类型分岔 —— 仅退款是"向平台退款、作废订单并发还库存、券与积分"；
     * 退货退款是"同意退"，把商家的退货地址写进申请单，等会员寄回。拒绝必须写理由。</p>
     *
     * <p>平台退款失败时返回 {@code ORDER_REFUND_FAILED}（17017），此时订单、库存、券与积分
     * 一个都没动，重试一次即可。</p>
     *
     * @param request 当前请求，用于取出登录管理员
     * @param vo 审核参数，申请单 ID、通过与拒绝、拒绝理由（拒绝时必填）
     * @return 统一成功响应，不含业务数据
     */
    @PostMapping("/audit")
    public R<Void> audit(HttpServletRequest request, @Valid @RequestBody AfterSaleAuditVo vo) {
        String operator = LoginUserUtils.requireCurrentAdmin(request).getUsername();
        orderService.auditAfterSale(vo, operator);

        return R.ok();
    }

    /**
     * 确认收到退货：把"退货中"的退货单置为已完成，随后向平台发起退款。
     *
     * <p>只有"退货中"的单能收货：没通过审核的单收不了货，重复点也不会重复退款。收货人取当前操作人。</p>
     *
     * <p><b>这一步不加库存</b>：退回来的货不能当新货卖，所以可售库存不增加。</p>
     *
     * @param request 当前请求，用于取出登录管理员
     * @param vo 收货参数，申请单 ID 与收货备注
     * @return 统一成功响应，不含业务数据
     */
    @PostMapping("/receive")
    public R<Void> receive(HttpServletRequest request, @Valid @RequestBody AfterSaleReceiveVo vo) {
        String operator = LoginUserUtils.requireCurrentAdmin(request).getUsername();
        orderService.receiveReturn(vo.getId(), vo.getReceiveNote(), operator);

        return R.ok();
    }
}
