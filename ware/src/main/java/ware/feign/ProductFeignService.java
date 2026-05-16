package ware.feign;

import common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@FeignClient("product")
public interface ProductFeignService {
    @RequestMapping("/product/skuinfo/info/{skuId}")
    R getProduct(@PathVariable Long skuId);

    @PostMapping(value = "/product/skuinfo/getSkuNames")
    R getSkuNames(@RequestBody List<Long> spuIds);
}
