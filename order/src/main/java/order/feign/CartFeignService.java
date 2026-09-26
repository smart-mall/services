package order.feign;

import common.utils.R;
import order.vo.OrderItemVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
/** 购物车远程调用。要带上 {@code X-Member-Claims}（FeignConfig 会转发），否则 cart 返回 401。 */
@FeignClient("cart")
public interface CartFeignService {

    /**
     * 查询当前用户购物车中已勾选的商品项（下单确认页用），价格是商品服务里的最新价。
     *
     * @return {@code data} 是 {@code List<OrderItemVo>}；购物车为空时是空数组，不是 null
     */
    @GetMapping(value = "/cart/front/jwt/checked")
    R<List<OrderItemVo>> getCheckedItems();

}
