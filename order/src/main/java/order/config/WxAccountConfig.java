package order.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;


/**
 * 微信支付账号参数，绑定 {@code wx} 前缀，由 {@link BestPayConfig} 读取后装配成 BestPay 的支付参数。
 */
@Component
@ConfigurationProperties(prefix = "wx")
@Data
public class WxAccountConfig {

    /** 微信应用 ID。 */
    private String appId;

    /** 微信支付商户号。 */
    private String mchId;

    /** 微信支付商户 API 密钥，用于请求签名。 */
    private String mchKey;

    /** 微信异步通知地址，由微信服务端回调。 */
    private String notifyUrl;

    /** 支付完成后浏览器跳转地址。 */
    private String returnUrl;

}
