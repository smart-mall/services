package order.web;

import com.alipay.api.AlipayApiException;
import com.alipay.api.internal.util.AlipaySignature;
import jakarta.servlet.http.HttpServletRequest;
import order.config.AlipayTemplate;
import order.service.OrderService;
import order.vo.PayAsyncVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

/**
 * 支付宝与微信的支付异步通知入口，由外部支付系统在支付结果确定后回调。
 *
 * <p>回调没有登录态，所以挂在 {@code order/front} 约定下的公开路径上，地址配在 application-common.yaml
 * 的 {@code alipay.notify_url} 与 {@code wx.notifyUrl}。返回值是给支付网关看的纯文本，不能用 {@code R} 包装：
 * 支付宝只认字面量 {@code success}，微信只认约定的 XML 应答，返回其它内容会被视为处理失败并重复通知。
 */
@RestController
public class OrderPayedController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private AlipayTemplate alipayTemplate;

    /**
     * 处理支付宝异步通知：验签通过后写入交易流水，并把订单置为已付款。
     *
     * <p>验签用 {@code alipay} 前缀下的公钥、编码与签名算法对原始表单参数做签名校验，未通过直接返回
     * {@code error}。本方法不做幂等去重，支付宝重复通知会再次写入一条 {@code oms_payment_info}。
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
     * 处理微信异步通知：交给 {@code OrderService#asyncNotify} 验签、校验订单状态并置为已付款。
     *
     * <p>入参是微信 POST 的原始报文，返回值必须是微信约定的应答 XML；已支付或已取消的订单会抛异常，
     * 因此重复通知不会被再次入账。
     *
     * @param notifyData 微信回调的原始报文，验签在 BestPay SDK 内完成
     * @return 微信要求的应答 XML，{@code return_code} 为 {@code SUCCESS}
     */
    @PostMapping(value = "/order/front/notify/wx")
    public String asyncNotify(@RequestBody String notifyData) {
        return orderService.asyncNotify(notifyData);
    }

}
