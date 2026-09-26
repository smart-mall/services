package ware.feign;

import common.utils.R;
import ware.vo.OrderVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;


@FeignClient("order")
public interface OrderFeignService {

    @GetMapping(value = "/order/order/status/{orderSn}")
    R<OrderVo> getOrderStatus(@PathVariable("orderSn") String orderSn);

}
