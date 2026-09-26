package order.feign;

import order.vo.PayVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;


/**
 * third-party 服务的远程调用接口。
 *
 * <p>order 侧当前没有调用方。
 */
@FeignClient("third-party")
public interface ThridFeignService {

    /**
     * 请求 third-party 的支付接口，原样返回响应体。
     *
     * <p>返回的是字符串而不是 {@code R}，调用失败由 Feign 抛异常。
     *
     * @param vo 支付参数，含商户订单号、订单名称与金额，商品描述可空；不能为 {@code null}
     * @return 响应体原文；Feign 调用失败时抛出异常，不通过返回值表达错误
     */
    @GetMapping(value = "/pay",consumes = "application/json")
    String pay(@RequestBody PayVo vo);

}
