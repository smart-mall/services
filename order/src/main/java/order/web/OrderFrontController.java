package order.web;

import common.utils.R;
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

/**
 * 订单前台接口（给 Vue 用）。
 *
 * <p>取代原来那三个返回视图的控制器：{@code OrderWebController}（/toTrade 渲染 confirm 模板、
 * /submitOrder 渲染 pay 模板）和 {@code PayWebController}（/aliPayOrder 直接吐 text/html）。
 * 前台换成 Vue、请求变成跨源 XHR 之后，视图名和整页跳转都用不了了，模板也全部删除。</p>
 *
 * <p><b>路径为什么是 {@code order/front}</b>：网关的 order-route 是
 * {@code Path=/api/order/**} + {@code RewritePath=/api/(?<segment>.*),/$\{segment\}}，
 * 所以 SPA 调 {@code /api/order/front/confirm} 到服务里就是 {@code /order/front/confirm}。
 * 中间那层 {@code front} 和 {@code product/front}、{@code search/front} 保持一致。
 * 另外注意不要写成 {@code /front/{orderSn}} 这种直接挂变量的形式 —— 那样 list/fare/confirm
 * 这几个字面量路径都要靠"字面量优先于变量"这条匹配规则才不出错，显式多一级更稳。</p>
 *
 * <p><b>认证与归属</b>：{@code LoginUserInterceptor} 拦 {@code /**}，未登录统一返回真 HTTP 401。
 * 但"订单是不是你的/地址是不是你的"它管不了，由 service 层逐个校验
 * （{@code requireOwnOrder} / {@code requireOwnAddress}）—— 这几个接口原来只校验登录不校验归属，
 * 拿到别人的订单号就能发起支付或读到收货人信息。</p>
 *
 * <p>支付回调 {@code /payed/notify}、{@code /pay/notify} 不在这里，它们是第三方服务器的
 * 固定地址，仍在 {@code OrderPayedController} 里、走 Host 路由。</p>
 */
@RestController
@RequestMapping("order/front")
public class OrderFrontController {

    private final OrderService orderService;

    public OrderFrontController(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * 结算页初始化：收货地址列表、已勾选购物项、库存、积分、防重令牌、金额。
     *
     * <p>三个金额字段（totalAmount / freightAmount / payAmount）都由后端算好，
     * 前端只显示和原样回传 payAmount —— 提交时要拿它和重新算出来的金额比对。</p>
     */
    @GetMapping("/confirm")
    public R confirm() {
        return R.ok().setData(orderService.confirmOrder());
    }

    /**
     * 换收货地址时重算运费。
     *
     * <p>老页面是浏览器直接去调 ware 的 {@code /api/ware/wareinfo/fare}，再用 JS 把
     * "商品总额 + 运费"加出来。那样既绕过了地址归属校验，又让前端用浮点数算钱，
     * 这里收口到 order，顺便把归属校验补上。</p>
     */
    @GetMapping("/fare")
    public R fare(@RequestParam("addrId") Long addrId) {
        return R.ok().setData(orderService.getFare(addrId));
    }

    /**
     * 提交订单。
     *
     * <p>失败全部走 {@code R.error(code, msg)}。原先 {@code SubmitOrderResponseVo} 里有个
     * 用 1/2/3 表示失败原因的 {@code code}，和 {@code R.code} 是两个命名空间，套在一起
     * 前端会漏判成功；那个字段已经删掉。</p>
     */
    @PostMapping("/submit")
    public R submit(@Valid @RequestBody OrderSubmitVo vo) {
        return R.ok().setData(orderService.submitOrder(vo));
    }

    /**
     * 我的订单分页。参数：{@code pageNum} / {@code pageSize} / {@code status}（可选）。
     *
     * <p>返回体的 {@code data} 是 PageUtils：{@code {totalCount, pageSize, totalPage, currPage, list}}，
     * 其中 {@code list} 的每一项是订单，带 {@code orderItemEntityList} 和 {@code statusText}。</p>
     */
    @GetMapping("/list")
    public R list(@RequestParam Map<String, Object> params) {
        return R.ok().setData(orderService.queryMemberOrders(params));
    }

    /** 订单详情（含订单项） */
    @GetMapping("/detail/{orderSn}")
    public R detail(@PathVariable("orderSn") String orderSn) {
        return R.ok().setData(orderService.getOrderDetail(orderSn));
    }

    /**
     * 发起支付。
     *
     * <p>返回 {@code PayResultVo}：支付宝给 {@code form}（一整段 HTML 表单，前端写进新窗口自动提交），
     * 微信给 {@code codeUrl}（渲染成二维码后由前端轮询 {@code /status/{orderSn}}）。</p>
     */
    @PostMapping("/pay/{orderSn}")
    public R pay(@PathVariable("orderSn") String orderSn, @Valid @RequestBody PayRequestVo vo) {
        return R.ok().setData(orderService.payOrder(orderSn, vo.getPayType()));
    }

    /** 查自己的订单状态，供扫码页轮询 */
    @GetMapping("/status/{orderSn}")
    public R status(@PathVariable("orderSn") String orderSn) {
        return R.ok().setData(orderService.getMyOrderStatus(orderSn));
    }

    /** 取消未支付的订单，同时通知仓库释放库存 */
    @PutMapping("/cancel/{orderSn}")
    public R cancel(@PathVariable("orderSn") String orderSn) {
        orderService.cancelOrder(orderSn);
        return R.ok();
    }

}
