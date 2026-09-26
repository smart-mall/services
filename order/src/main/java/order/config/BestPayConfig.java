package order.config;

import com.lly835.bestpay.config.WxPayConfig;
import com.lly835.bestpay.service.BestPayService;
import com.lly835.bestpay.service.impl.BestPayServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Best Pay 支付配置类
 */

@Configuration
public class BestPayConfig {

    private final WxAccountConfig wxAccountConfig;

    public BestPayConfig(WxAccountConfig wxAccountConfig) {
        this.wxAccountConfig = wxAccountConfig;
    }

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

    @Bean
    public BestPayService bestPayService(WxPayConfig wxPayConfig) {
        BestPayServiceImpl bestPayService = new BestPayServiceImpl();
        bestPayService.setWxPayConfig(wxPayConfig);
        return bestPayService;
    }
}