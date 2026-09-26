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
 * 第三方支付的异步通知。没有登录态，所以挂在 front 约定下的公开路径上；
 * 响应必须是字面量 {@code success}，不能用 {@code R} 包。回调地址配在 application-common.yaml。
 */
@RestController
public class OrderPayedController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private AlipayTemplate alipayTemplate;

    @PostMapping(value = "/order/front/notify/alipay")
    public String handleAlipayed(PayAsyncVo asyncVo, HttpServletRequest request) throws AlipayApiException, UnsupportedEncodingException {
        // 只要收到支付宝的异步通知，返回 success 支付宝便不再通知
        // 获取支付宝POST过来反馈信息
        //TODO 需要验签
        Map<String, String> params = new HashMap<>();
        Map<String, String[]> requestParams = request.getParameterMap();
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
                alipayTemplate.getCharset(), alipayTemplate.getSign_type()); //调用SDK验证签名

        if (signVerified) {
            System.out.println("签名验证成功...");
            //去修改订单状态
            String result = orderService.handlePayResult(asyncVo);
            return result;
        } else {
            System.out.println("签名验证失败...");
            return "error";
        }
    }

    @PostMapping(value = "/order/front/notify/wx")
    public String asyncNotify(@RequestBody String notifyData) {
        //异步通知结果
        return orderService.asyncNotify(notifyData);
    }

}
