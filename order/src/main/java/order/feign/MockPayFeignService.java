package order.feign;

import order.vo.MockRefundRequestVo;
import order.vo.MockRefundResultVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * 模拟支付平台的退款接口，扮演"第三方平台的退款 API"。
 *
 * <p><b>方向与支付相反</b>：支付是"用户跳转 → 平台 → 回调商户"，退款是"商户服务端 → 平台退款 API"，
 * 没有浏览器参与、也没有回调 —— 平台在同一次请求-应答里给结论（支付宝的形状；微信是"先受理、
 * 再异步通知"，那种形状本项目没有模拟，理由见 {@code mock-pay/nginx.conf} 的说明）。
 *
 * <p><b>为什么不走网关</b>：这是商户向第三方平台发起的对外调用。真实世界里商户直连平台，
 * 不经过自家的网关；这里的上游是同一个 compose 网络里的 {@code gl-mock-pay} 容器，
 * 地址前缀来自 {@code gl.pay.mock.platform-url}，不经过 {@code gl-gateway}。
 * 配置里只放服务地址，接口路径 {@code /refund} 写在本接口上 —— 与真实渠道一个形状：
 * 换环境只换前缀，路径不变（把完整地址配进来，Feign 会在它后面再拼一次路径，变成 /refund/refund）。
 *
 * <p><b>商户身份怎么给</b>：真实渠道靠资质 —— 微信要商户 API 证书做双向 TLS，支付宝要应用私钥签名。
 * 平台替身用一个固定头 {@code X-Mock-Merchant-Sign} 表达同一件事，值必须与 {@code mock-pay/nginx.conf}
 * 里的判断一致（改名要两边一起改）；不带或不对时平台直接 403，这里会转成退款失败。
 */
@FeignClient(name = "mock-pay-refund", url = "${gl.pay.mock.platform-url}")
public interface MockPayFeignService {

    /**
     * 向平台发起退款。
     *
     * @param merchantSign 商户身份凭证，取 {@code gl.pay.mock.merchant-sign}，不能为 {@code null}
     * @param request 退款请求，含商户退款单号、退款金额与原支付交易号，均不能为空
     * @return 平台应答；平台返回非 2xx（例如凭证不对时的 403）时本调用直接抛异常
     */
    @PostMapping("/refund")
    MockRefundResultVo refund(@RequestHeader("X-Mock-Merchant-Sign") String merchantSign,
                              @RequestBody MockRefundRequestVo request);
}
