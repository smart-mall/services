package ware.feign;

import common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.Map;

/**
 * 商品服务的 Feign 客户端：按 SKU 反查商品是否存在，以及批量取 SKU 名称。
 *
 * <p>跨服务契约，路径与参数名以商品服务 {@code SkuInfoController} 的签名为准。
 */
@FeignClient("product")
public interface ProductFeignService {

    /**
     * 查询单个 SKU 的商品信息。
     *
     * <p>返回的是商品服务原始的字段 Map，调用方只做存在性判断，不解析具体字段。</p>
     *
     * @param skuId 商品 SKU ID，不能为 {@code null}
     * @return 商品字段；SKU 不存在时 {@code code} 非 0 或 {@code data} 为 {@code null}
     */
    @RequestMapping("/product/skuinfo/info/{skuId}")
    R<Map<String, Object>> getProduct(@PathVariable Long skuId);

    /**
     * 批量查询 SKU 名称。
     *
     * <p>入参语义是 SKU ID（调用方传的就是 SKU ID），参数名沿用商品服务侧签名，不要按名字当成 SPU ID。</p>
     *
     * @param spuIds 商品 SKU ID 列表
     * @return SKU ID 到 SKU 名称的映射；查不到的 SKU 不会出现在结果里
     */
    @PostMapping(value = "/product/skuinfo/getSkuNames")
    R<Map<Long, String>> getSkuNames(@RequestBody List<Long> spuIds);
}
