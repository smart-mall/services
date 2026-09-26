package auth.utils;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;


/**
 * 微信开放平台配置的静态持有者：把 {@code @Value} 注入的实例字段抄到静态字段上。
 *
 * <p>三个静态字段在容器完成 {@link #afterPropertiesSet()} 之后才可用，早于此时读取拿到的是 null；
 * 当前模块内没有引用方。
 */
@Component
public class ConstantWxUtils implements InitializingBean {

    /** 微信开放平台应用的 app_id */
    @Value("${wx.open.app_id}")
    private String appId;

    /** 微信开放平台应用的 app_secret */
    @Value("${wx.open.app_secret}")
    private String appSecret;

    /** 授权后跳回的业务地址，须与开放平台后台登记的一致 */
    @Value("${wx.open.redirect_url}")
    private String redirectUrl;

    /** 应用 app_id，由 {@link #afterPropertiesSet()} 从 {@link #appId} 抄入 */
    public static String WX_OPEN_APP_ID;

    /** 应用 app_secret，由 {@link #afterPropertiesSet()} 从 {@link #appSecret} 抄入 */
    public static String WX_OPEN_APP_SECRET;

    /** 授权回调地址，由 {@link #afterPropertiesSet()} 从 {@link #redirectUrl} 抄入 */
    public static String WX_OPEN_REDIRECT_URL;

    /** {@inheritDoc} */
    @Override
    public void afterPropertiesSet() throws Exception {
        WX_OPEN_APP_ID = appId;
        WX_OPEN_APP_SECRET = appSecret;
        WX_OPEN_REDIRECT_URL = redirectUrl;
    }
}
