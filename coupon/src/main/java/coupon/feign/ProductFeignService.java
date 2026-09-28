package coupon.feign;

import common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

import java.util.Map;
/**
 * product 商品服务的 Feign 契约，按 id 批量取 SPU、SKU 名称。
 *
 * <p>返回值统一是 {@link R}：调用方必须先判 {@code code}，为 0 时才能读 {@code data}。
 */
@FeignClient("product")
public interface ProductFeignService {

    /**
     * 按 SPU id 批量查询 SPU 名称。
     *
     * @param spuIds SPU id 集合，不能为 {@code null}
     * @return 统一响应；{@code code} 为 0 时 {@code data} 是 spuId 到 SPU 名称的映射，查不到的 id 不在映射中
     *         （调用方取值得到 {@code null}）；{@code code} 非 0 时 {@code data} 为 {@code null}
     */
    @PostMapping(value = "/product/spuinfo/getSpuNames")
    R<Map<Long, String>> getSpuNames(@RequestBody List<Long> spuIds);

    /**
     * 按 SKU id 批量查询 SKU 名称。
     *
     * @param spuIds SKU id 集合，不能为 {@code null}；形参名沿用 {@code spuIds}，语义按 SKU 理解
     * @return 统一响应；{@code code} 为 0 时 {@code data} 是 skuId 到 SKU 名称的映射，查不到的 id 不在映射中
     *         （调用方取值得到 {@code null}）；{@code code} 非 0 时 {@code data} 为 {@code null}
     */
    @PostMapping(value = "/product/skuinfo/getSkuNames")
    R<Map<Long, String>> getSkuNames(@RequestBody List<Long> spuIds);
}
