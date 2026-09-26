package order.feign;

import common.utils.R;
import order.vo.SkuInfoVo;
import order.vo.SpuInfoVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;


@FeignClient("product")
public interface ProductFeignService {

    /**
     * 根据skuId查询spu的信息
     * @param skuId
     * @return
     */
    @GetMapping(value = "/product/spuinfo/skuId/{skuId}")
    public R<SpuInfoVo> getSpuInfoBySkuId(@PathVariable("skuId") Long skuId);

    @RequestMapping("/product/skuinfo/info/{skuId}")
    //@RequiresPermissions("product:skuinfo:info")
    public R<SkuInfoVo> getSkuInfoBySkuId(@PathVariable("skuId") Long skuId);

}
