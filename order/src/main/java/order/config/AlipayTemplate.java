package order.config;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.AlipayTradePagePayRequest;
import lombok.Data;
import order.vo.PayVo;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 支付宝电脑网站支付配置，绑定 {@code alipay} 前缀。
 *
 * <p>由 {@code OrderServiceImpl#payOrder} 调用 {@link #pay(PayVo)} 拉起收银台；
 * {@code OrderPayedController} 用其中的公钥、编码与签名算法对回调验签。
 */
@ConfigurationProperties(prefix = "alipay")
@Component
@Data
public class AlipayTemplate {

    /** 支付宝应用 ID（APPID）。 */
    public String app_id;

    /** 商户私钥，PKCS8 格式的 RSA2 私钥。 */
    public String merchant_private_key;

    /** 支付宝公钥，回调验签时使用，取值对应 APPID 下的公钥。 */
    public String alipay_public_key;

    /** 支付宝异步通知地址，必须是外网可访问的完整 http 地址，且不能带自定义查询参数。 */
    public String notify_url;

    /** 支付完成后浏览器同步跳转地址，同样必须是完整 http 地址、不能带自定义查询参数。 */
    public String return_url;

    /** 签名算法，SDK 发起支付与回调验签共用。 */
    private  String sign_type;

    /** 字符编码，SDK 发起支付与回调验签共用。 */
    private  String charset;

    /** 支付宝侧订单超时时间，原样写入 bizContent 的 {@code timeout_express}。 */
    private String timeout = "1m";

    /** 支付宝网关地址，沙箱为 {@code https://openapi.alipaydev.com/gateway.do}。 */
    public String gatewayUrl;

    /**
     * 发起支付宝电脑网站支付。
     *
     * <p>商户订单号、金额与商品名取自 {@code vo}，其中订单号与金额由后端按库里的订单生成，不接受前端传值。
     *
     * @param vo 支付宝下单参数，{@code out_trade_no}、{@code total_amount}、{@code subject} 必填
     * @return 支付宝返回的完整 HTML 页面，浏览器渲染后自动进入收银台
     * @throws AlipayApiException 调用支付宝网关失败或返回报文无法解析时抛出
     */
    public  String pay(PayVo vo) throws AlipayApiException {

        AlipayClient alipayClient = new DefaultAlipayClient(gatewayUrl,
                app_id, merchant_private_key, "json",
                charset, alipay_public_key, sign_type);

        AlipayTradePagePayRequest alipayRequest = new AlipayTradePagePayRequest();
        alipayRequest.setReturnUrl(return_url);
        alipayRequest.setNotifyUrl(notify_url);

        String out_trade_no = vo.getOut_trade_no();
        String total_amount = vo.getTotal_amount();
        String subject = vo.getSubject();
        String body = vo.getBody();

        // bizContent 的键名由支付宝网关约定，只能按字面量拼 JSON，不能改名
        alipayRequest.setBizContent("{\"out_trade_no\":\""+ out_trade_no +"\","
                + "\"total_amount\":\""+ total_amount +"\","
                + "\"subject\":\""+ subject +"\","
                + "\"body\":\""+ body +"\","
                + "\"timeout_express\":\""+timeout+"\","
                + "\"product_code\":\"FAST_INSTANT_TRADE_PAY\"}");

        String result = alipayClient.pageExecute(alipayRequest).getBody();

        System.out.println("支付宝的响应："+result);

        return result;

    }
}
