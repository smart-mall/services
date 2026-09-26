package ware.feign;

import common.utils.R;
import ware.vo.OrderVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;


/**
 * 订单服务的 Feign 客户端：按订单号反查订单状态，供库存解锁前判断订单是否已关闭。
 */
@FeignClient("order")
public interface OrderFeignService {

    /**
     * 按订单号查询订单信息。
     *
     * <p>调用方只取 {@code status} 判断订单是否已关闭（4 为已关闭），据此决定能否解锁库存。</p>
     *
     * @param orderSn 订单号，不能为 {@code null}
     * @return 订单信息；订单不存在时 {@code data} 为 {@code null}
     */
    @GetMapping(value = "/order/order/status/{orderSn}")
    R<OrderVo> getOrderStatus(@PathVariable("orderSn") String orderSn);

}
