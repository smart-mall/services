package order.feign;

import common.utils.R;
import order.vo.OrderItemVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
/**
 * 购物车服务的远程调用接口。
 *
 * <p>cart 的接口要求登录：Feign 调用不继承上游请求头，身份由 {@code common.config.FeignConfig}
 * 从当前请求里取 {@code X-Member-Claims} 转发，缺少它 cart 返回 401。
 */
@FeignClient("cart")
public interface CartFeignService {

    /**
     * 查询当前会员购物车中已勾选的商品项，价格已刷新为商品服务的最新价。
     *
     * <p>供结算页与提交订单组装订单项使用。
     *
     * @return {@code data} 为已勾选的购物项，没有勾选项时为空列表；
     *         cart 返回失败（{@code code} 非 0）时 {@code data} 为 {@code null}
     */
    @GetMapping(value = "/cart/front/jwt/checked")
    R<List<OrderItemVo>> getCheckedItems();

}
