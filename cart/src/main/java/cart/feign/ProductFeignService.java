package cart.feign;

import cart.vo.SkuInfoVo;
import common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.math.BigDecimal;
import java.util.List;

/**
 * 商品服务的 Feign 客户端，供购物车查询 SKU 信息、销售属性与最新价格。
 *
 * <p>方法上的路径与商品服务 controller 的映射一一对应，改动需两侧同步。
 */
@FeignClient("product")
public interface ProductFeignService {

    /**
     * 查询 SKU 基本信息。
     *
     * @param skuId 商品 SKU 标识，不能为 {@code null}
     * @return 统一响应；SKU 不存在时 {@code data} 为 {@code null}，调用方必须判空
     */
    @RequestMapping("/product/skuinfo/info/{skuId}")
    R<SkuInfoVo> getInfo(@PathVariable("skuId") Long skuId);

    /**
     * 查询 SKU 的销售属性值列表。
     *
     * @param skuId 商品 SKU 标识，不能为 {@code null}
     * @return 销售属性值，每项形如 {@code 颜色：黑色}
     */
    @GetMapping(value = "/product/skusaleattrvalue/stringList/{skuId}")
    List<String> getSkuSaleAttrValues(@PathVariable("skuId") Long skuId);

    /**
     * 查询 SKU 当前的最新价格。
     *
     * @param skuId 商品 SKU 标识，不能为 {@code null}
     * @return 最新价格
     */
    @GetMapping(value = "/product/skuinfo/{skuId}/price")
    BigDecimal getPrice(@PathVariable("skuId") Long skuId);
}
