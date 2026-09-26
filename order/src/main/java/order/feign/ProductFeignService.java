package order.feign;

import common.utils.R;
import order.vo.SkuInfoVo;
import order.vo.SpuInfoVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;


/**
 * 商品服务的远程调用接口，用于补齐订单项上的 SPU 与 SKU 信息。
 */
@FeignClient("product")
public interface ProductFeignService {

    /**
     * 按 SKU 查询它所属的 SPU 信息。
     *
     * @param skuId 商品 SKU 标识，不能为 {@code null}
     * @return {@code data} 为 SPU 信息；product 返回失败（{@code code} 非 0）时 {@code data} 为 {@code null}
     */
    @GetMapping(value = "/product/spuinfo/skuId/{skuId}")
    public R<SpuInfoVo> getSpuInfoBySkuId(@PathVariable("skuId") Long skuId);

    /**
     * 按 SKU 查询 SKU 自身的名称、图片与价格。
     *
     * @param skuId 商品 SKU 标识，不能为 {@code null}
     * @return {@code data} 为 SKU 信息；SKU 不存在或 product 返回失败时 {@code data} 为 {@code null}
     */
    @RequestMapping("/product/skuinfo/info/{skuId}")
    public R<SkuInfoVo> getSkuInfoBySkuId(@PathVariable("skuId") Long skuId);

}
