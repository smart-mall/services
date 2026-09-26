package seckill.feign;

import common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import seckill.vo.SkuInfoVo;
@FeignClient("product")
public interface ProductFeignService {

    @RequestMapping("/product/skuinfo/info/{skuId}")
    R<SkuInfoVo> getSkuInfo(@PathVariable("skuId") Long skuId);

}
