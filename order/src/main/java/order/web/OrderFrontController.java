package order.web;

import common.utils.LoginUserUtils;
import common.utils.R;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import order.service.OrderService;
import order.vo.OrderSubmitVo;
import order.vo.PayRequestVo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import common.vo.PageVO;
import order.entity.OrderEntity;
import order.vo.OrderConfirmVo;
import order.vo.SubmitOrderResponseVo;
import order.vo.FareVo;
import order.vo.PayResultVo;
import order.vo.OrderStatusVo;
import order.vo.OrderPageQuery;
/**
 * 订单前台接口（给 Vue 用），全部要求登录。"订单 / 地址是不是你的"不在这里管，
 * 由 service 层的 {@code requireOwnOrder} / {@code requireOwnAddress} 逐个校验。
 */
@RestController
@RequestMapping("order/front/jwt")
public class OrderFrontController {

    private final OrderService orderService;

    /**
     * 注入订单服务。
     *
     * @param orderService 订单服务
     */
    public OrderFrontController(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * 结算页初始化：返回收货地址列表、已勾选购物项、库存、积分、防重令牌与金额。
     *
     * <p>三个金额字段（totalAmount / freightAmount / payAmount）都由后端算好，前端只显示和原样回传 payAmount ——
     * 提交时要拿它和重新算出来的金额比对。
     *
     * @param request 当前请求，用于取出登录用户
     * @return 结算页数据
     */
    @GetMapping("/confirm")
    public R<OrderConfirmVo> confirm(HttpServletRequest request) {
        return R.ok(orderService.confirmOrder(LoginUserUtils.requireCurrentUser(request)));
    }

    /**
     * 换收货地址时重算运费。
     *
     * <p>收口在 order 是为了带上地址归属校验，也不让前端算钱。
     *
     * @param request 当前请求，用于取出登录用户
     * @param addrId 收货地址主键，必须是当前用户自己的地址
     * @return 运费信息
     */
    @GetMapping("/fare")
    public R<FareVo> fare(HttpServletRequest request, @RequestParam("addrId") Long addrId) {
        return R.ok(orderService.getFare(LoginUserUtils.requireCurrentUser(request), addrId));
    }

    /**
     * 提交订单。
     *
     * <p>失败全部走 {@code R.error(code, msg)}。
     *
     * @param request 当前请求，用于取出登录用户
     * @param vo 订单提交参数，防重令牌与金额由后端校验
     * @return 提交结果
     */
    @PostMapping("/submit")
    public R<SubmitOrderResponseVo> submit(HttpServletRequest request, @Valid @RequestBody OrderSubmitVo vo) {
        return R.ok(orderService.submitOrder(LoginUserUtils.requireCurrentUser(request), vo));
    }

    /**
     * 分页查询当前用户的订单。
     *
     * <p>{@code page} / {@code limit} / {@code status}（可选）由 {@code OrderPageQuery} 绑定；
     * 返回的 {@code rows} 每一项是订单，带 {@code orderItemEntityList} 和 {@code statusText}。
     *
     * @param request 当前请求，用于取出登录用户
     * @param query 分页与状态筛选条件
     * @return 分页结果，{@code data} 为 {@code {total, rows}}
     */
    @GetMapping("/list")
    public R<PageVO<OrderEntity>> list(HttpServletRequest request, OrderPageQuery query) {
        return R.ok(orderService.queryMemberOrders(LoginUserUtils.requireCurrentUser(request), query));
    }

    /**
     * 查询当前用户的订单详情。
     *
     * @param request 当前请求，用于取出登录用户
     * @param orderSn 订单号，必须是当前用户自己的订单
     * @return 订单详情，含订单项
     */
    @GetMapping("/detail/{orderSn}")
    public R<OrderEntity> detail(HttpServletRequest request, @PathVariable("orderSn") String orderSn) {
        return R.ok(orderService.getOrderDetail(LoginUserUtils.requireCurrentUser(request), orderSn));
    }

    /**
     * 发起支付。
     *
     * <p>返回 {@code PayResultVo}：支付宝给 {@code form}（一整段 HTML 表单，前端写进新窗口自动提交），
     * 微信给 {@code codeUrl}（渲染成二维码后由前端轮询 {@code /status/{orderSn}}）。
     *
     * @param request 当前请求，用于取出登录用户
     * @param orderSn 订单号，必须处于待付款状态
     * @param vo 支付参数，{@code payType} 取 {@code PayConstant}
     * @return 支付参数，含支付宝表单或微信二维码链接
     */
    @PostMapping("/pay/{orderSn}")
    public R<PayResultVo> pay(HttpServletRequest request, @PathVariable("orderSn") String orderSn,
                 @Valid @RequestBody PayRequestVo vo) {
        return R.ok(orderService.payOrder(LoginUserUtils.requireCurrentUser(request), orderSn, vo.getPayType()));
    }

    /**
     * 查询当前用户的订单状态，供扫码页轮询。
     *
     * @param request 当前请求，用于取出登录用户
     * @param orderSn 订单号，必须是当前用户自己的订单
     * @return 订单状态，只带 orderSn、status 与 statusText
     */
    @GetMapping("/status/{orderSn}")
    public R<OrderStatusVo> status(HttpServletRequest request, @PathVariable("orderSn") String orderSn) {
        return R.ok(orderService.getMyOrderStatus(LoginUserUtils.requireCurrentUser(request), orderSn));
    }

    /**
     * 取消未支付的订单，同时通知仓库释放库存。
     *
     * @param request 当前请求，用于取出登录用户
     * @param orderSn 订单号，必须是当前用户自己的待付款订单
     * @return 成功响应，无数据
     */
    @PutMapping("/cancel/{orderSn}")
    public R<Void> cancel(HttpServletRequest request, @PathVariable("orderSn") String orderSn) {
        orderService.cancelOrder(LoginUserUtils.requireCurrentUser(request), orderSn);
        return R.ok();
    }
}
