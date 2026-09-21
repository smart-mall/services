package order.feign;

import common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 购物车远程调用。
 *
 * <p>路径从 {@code /currentUserCartItems} 换成了 {@code /cart/checked}：购物车控制器
 * 重写成 {@code @RestController} 之后，返回体统一套上了 {@code R} 信封
 * （原来那个裸 {@code List} 是全项目唯一的例外），旧路径已经删掉。</p>
 *
 * <p><b>调用能成功的前提是请求头里带着 {@code X-Member-Claims}</b>：
 * cart 的接口全部要求登录，而这个头是网关验签后注入、由 common 的 FeignConfig
 * 转发过来的。少了转发，这里会直接吃一个 401。</p>
 */
@FeignClient("cart")
public interface CartFeignService {

    /**
     * 查询当前用户购物车中已勾选的商品项（下单确认页用），价格是商品服务里的最新价。
     *
     * @return {@code data} 是 {@code List<OrderItemVo>}；购物车为空时是空数组，不是 null
     */
    @GetMapping(value = "/cart/checked")
    R getCheckedItems();

}
