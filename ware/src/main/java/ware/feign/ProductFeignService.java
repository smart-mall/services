package ware.feign;

import common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.Map;

@FeignClient("product")
public interface ProductFeignService {
    @RequestMapping("/product/skuinfo/info/{skuId}")
    R<Map<String, Object>> getProduct(@PathVariable Long skuId);

    @PostMapping(value = "/product/skuinfo/getSkuNames")
    R<Map<Long, String>> getSkuNames(@RequestBody List<Long> spuIds);
}
