package order.config;

import com.lly835.bestpay.config.WxPayConfig;
import com.lly835.bestpay.service.BestPayService;
import com.lly835.bestpay.service.impl.BestPayServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 微信支付配置：把 {@code wx} 前缀绑定的账号参数装配成 BestPay 的 {@link WxPayConfig} 与 {@link BestPayService}。
 *
 * <p>不带配置前缀，由组件扫描装配；{@code OrderServiceImpl} 注入 {@link BestPayService} 发起支付与解析微信回调。
 */

@Configuration
public class BestPayConfig {

    /** 微信支付账号参数，来自 {@code wx} 前缀。 */
    private final WxAccountConfig wxAccountConfig;

    /**
     * 注入微信支付账号参数。
     *
     * @param wxAccountConfig 由 {@code wx} 前缀绑定的账号参数
     */
    public BestPayConfig(WxAccountConfig wxAccountConfig) {
        this.wxAccountConfig = wxAccountConfig;
    }

    /**
     * 构造 BestPay 的微信支付参数。
     *
     * @return 已填入 appId、mchId、mchKey 与回调地址的微信支付参数
     */
    @Bean
    public WxPayConfig wxPayConfig() {
        WxPayConfig wxPayConfig = new WxPayConfig();
        wxPayConfig.setAppId(wxAccountConfig.getAppId());
        wxPayConfig.setMchId(wxAccountConfig.getMchId());
        wxPayConfig.setMchKey(wxAccountConfig.getMchKey());
        wxPayConfig.setNotifyUrl(wxAccountConfig.getNotifyUrl());
        wxPayConfig.setReturnUrl(wxAccountConfig.getReturnUrl());
        return wxPayConfig;
    }

    /**
     * 构造 BestPay 支付服务，供发起微信支付与解析微信回调使用。
     *
     * @param wxPayConfig 微信支付参数
     * @return 绑定了微信支付参数的 BestPay 服务
     */
    @Bean
    public BestPayService bestPayService(WxPayConfig wxPayConfig) {
        BestPayServiceImpl bestPayService = new BestPayServiceImpl();
        bestPayService.setWxPayConfig(wxPayConfig);
        return bestPayService;
    }
}