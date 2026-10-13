package order.web;

import com.alipay.api.AlipayApiException;
import com.alipay.api.internal.util.AlipaySignature;
import common.exception.BaseException;
import jakarta.servlet.http.HttpServletRequest;
import order.config.AlipayTemplate;
import order.service.OrderService;
import order.vo.PayAsyncVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * 支付宝、微信与测试支付的异步通知入口，三个渠道的通知都由本类接收。
 *
 * <p>回调没有登录态，所以都挂在 {@code order/front} 约定下的公开路径上。支付宝与微信的回调地址配在
 * application-common.yaml 的 {@code alipay.notify_url} 与 {@code wx.notifyUrl}；测试支付的回调地址由
 * 前端按网关地址拼出，它在 order 自己的 {@code gl.pay.mock.return-url} 里另配了提交后的回跳地址。
 *
 * <p>返回值面向的调用方不同：支付宝与微信的应答是给支付网关看的纯文本，不能用 {@code R} 包装 ——
 * 支付宝只认字面量 {@code success}，微信只认约定的 XML 应答，返回其它内容会被视为处理失败并重复通知；
 * 测试支付那条由浏览器整页提交，按 POST-重定向-GET 回跳前端的收银台页，服务端不生成任何页面。
 */
@RestController
public class OrderPayedController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private AlipayTemplate alipayTemplate;

    /**
     * 测试支付提交后回跳的前端地址，取 {@code gl.pay.mock.return-url}。
     *
     * <p>取自配置而不是请求参数，因此不构成开放重定向。
     */
    @Value("${gl.pay.mock.return-url}")
    private String mockReturnUrl;

    /** 平台转发回调时带的固定凭证，取 {@code gl.pay.mock.platform-sign}，与 mock-pay/nginx.conf 同值。 */
    @Value("${gl.pay.mock.platform-sign}")
    private String mockPlatformSign;

    /**
     * 处理支付宝异步通知：验签通过后写入交易流水，并把订单置为已付款。
     *
     * <p>验签用 {@code alipay} 前缀下的公钥、编码与签名算法对原始表单参数做签名校验，未通过直接返回
     * {@code error}。幂等由服务层保证：同一个交易号只落一条流水，订单只在待付款时被推进。
     *
     * @param asyncVo 表单绑定的通知参数，含 out_trade_no、trade_no 与 trade_status
     * @param request 原始请求，验签需要完整参数表
     * @return 处理结果；返回 {@code success} 后支付宝不再重复通知，验签失败返回 {@code error}
     * @throws AlipayApiException 验签调用失败时抛出
     */
    @PostMapping(value = "/order/front/notify/alipay")
    public String handleAlipayed(PayAsyncVo asyncVo, HttpServletRequest request) throws AlipayApiException, UnsupportedEncodingException {
        Map<String, String> params = new HashMap<>();
        Map<String, String[]> requestParams = request.getParameterMap();
        // 验签接口只接受 Map<String, String>，多值参数按支付宝约定用逗号拼成一个值
        for (String name : requestParams.keySet()) {
            String[] values = requestParams.get(name);
            String valueStr = "";
            for (int i = 0; i < values.length; i++) {
                valueStr = (i == values.length - 1) ? valueStr + values[i]
                        : valueStr + values[i] + ",";
            }
            params.put(name, valueStr);
        }

        boolean signVerified = AlipaySignature.rsaCheckV1(params, alipayTemplate.getAlipay_public_key(),
                alipayTemplate.getCharset(), alipayTemplate.getSign_type());

        if (signVerified) {
            System.out.println("签名验证成功...");
            String result = orderService.handlePayResult(asyncVo);
            return result;
        } else {
            System.out.println("签名验证失败...");
            return "error";
        }
    }

    /**
     * 处理微信异步通知：交给 {@code OrderService#asyncNotify} 验签、落交易流水、校验订单状态并置为已付款。
     *
     * <p>入参是微信 POST 的原始报文，返回值必须是微信约定的应答 XML；已支付或已取消的订单
     * 不改任何数据，但仍回 {@code SUCCESS}，因此重复通知不会被再次入账。
     *
     * @param notifyData 微信回调的原始报文，验签在 BestPay SDK 内完成
     * @return 微信要求的应答 XML，{@code return_code} 为 {@code SUCCESS}
     */
    @PostMapping(value = "/order/front/notify/wx")
    public String asyncNotify(@RequestBody String notifyData) {
        return orderService.asyncNotify(notifyData);
    }

    /**
     * 处理测试支付的确认通知：校验平台凭证与一次性票据后写入交易流水，并把订单置为已付款。
     *
     * <p>与前两条的差别在于送达方式：请求由浏览器整页提交，但途中经过平台容器，平台会补上
     * {@code X-Mock-Pay-Sign} 头；而"这次支付确实是本人在这个订单上发起的"由一次性票据证明，
     * 票据存 Redis、用掉即失效。
     *
     * <p>成功与拒绝都回跳，让结果页自己去读订单状态，拒绝原因由服务层记日志。回跳而不就地应答，
     * 是因为提交方是一次整页导航；而且这样刷新结果页是安全的 GET，不会重复提交。
     *
     * @param orderSn 订单号，来自表单隐藏域，不能为空
     * @param ticket 发起支付时下发的一次性票据，来自表单隐藏域，不能为空
     * @param platformSign 平台转发时补上的凭证，直连网关的请求没有这个头
     * @return 指向前端订单页的 302，查询参数 {@code mockPay} 为 {@code ok} 或 {@code failed}；
     *         凭证不符返回 403，订单号格式非法返回 400
     */
    @PostMapping(value = "/order/front/notify/mock")
    public ResponseEntity<Void> handleMockPayed(@RequestParam("orderSn") String orderSn,
                                                @RequestParam("ticket") String ticket,
                                                @RequestHeader(value = "X-Mock-Pay-Sign", required = false) String platformSign) {
        // 头名与 mock-pay/nginx.conf 里的 proxy_set_header 是同一份约定，改名要两边一起改。
        // nginx 会覆盖客户端自带的同名头，所以这个值只可能由平台容器写入
        if (!mockPlatformSign.equals(platformSign)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        // 订单号要拼进回跳地址的路径段，先按格式把路径穿越那类构造挡掉。
        // 真实订单号由 IdWorker 生成，恒为纯数字
        if (!orderSn.matches("\\d+")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        String outcome = "ok";
        try {
            orderService.handleMockPayResult(orderSn, ticket);
        } catch (BaseException e) {
            outcome = "failed";
        }

        URI location = UriComponentsBuilder.fromUriString(mockReturnUrl)
                .pathSegment(orderSn)
                .queryParam("mockPay", outcome)
                .build()
                .encode()
                .toUri();

        return ResponseEntity.status(HttpStatus.FOUND).location(location).build();
    }

}
