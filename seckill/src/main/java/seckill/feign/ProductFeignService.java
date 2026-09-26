package seckill.feign;

import common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import seckill.vo.SkuInfoVo;
/**
 * product 服务的 Feign 客户端：按 SKU ID 取商品基本信息，供上架时填充秒杀缓存。
 */
@FeignClient("product")
public interface ProductFeignService {

    /**
     * 查询指定 SKU 的基本信息。
     *
     * @param skuId 商品 SKU ID，不能为 {@code null}
     * @return SKU 基本信息，包在统一响应体里；调用方需先判断 {@code code} 为 0 再取 {@code data}
     */
    @RequestMapping("/product/skuinfo/info/{skuId}")
    R<SkuInfoVo> getSkuInfo(@PathVariable("skuId") Long skuId);

}
