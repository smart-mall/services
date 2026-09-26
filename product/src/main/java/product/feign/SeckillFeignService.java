package product.feign;

import common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import product.fallback.SeckillFeignServiceFallBack;


import product.vo.SeckillSkuVo;
@FeignClient(value = "seckill",fallback = SeckillFeignServiceFallBack.class)
public interface SeckillFeignService {
    @GetMapping(value = "/seckill/seckillsku/info/{skuId}")
    R<SeckillSkuVo> getSkuSeckilInfo(@PathVariable("skuId") Long skuId);

}
