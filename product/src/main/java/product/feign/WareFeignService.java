package product.feign;

import common.to.SkuDeleteBlockerTo;
import common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import product.vo.SkuHasStockVo;

import java.util.List;

/**
 * ware 服务的 Feign 客户端：查询 SKU 的可售库存，以及删除商品前的仓库侧守卫。
 *
 * <p>返回值统一是 {@link R}，业务是否成功看 {@code code}，取数必须读 {@code data}。
 */
@FeignClient("ware")
public interface WareFeignService {

    /**
     * 查询这些 SKU 是否有可售库存，可售量按 {@code stock - stock_locked} 汇总。
     *
     * @param skuIds SKU ID 列表，不能为 {@code null}；入参为空时返回空集合
     * @return 统一响应体，{@code data} 为每个入参 SKU 一条结果、顺序与入参一致；没有库存的 SKU 也会
     *         返回且 {@code hasStock} 为 {@code false}，调用失败时 {@code data} 为 {@code null}
     */
    @PostMapping("/ware/waresku/hasstock")
    R<List<SkuHasStockVo>> getSkusHasStock(@RequestBody List<Long> skuIds);

    /**
     * 判断这些 SKU 在仓库侧还能不能删，供删除商品前做守卫。
     *
     * <p>只读接口，不修改任何数据。
     *
     * @param skuIds 待删除的 SKU ID 列表，允许为 {@code null}，为空或全为 {@code null} 时返回空集合
     * @return 统一响应体，{@code data} 是阻塞清单：空集合表示都能删，非空时每项说明这个 sku 在哪个仓
     *         还有多少件、还有几条没走完的采购需求；调用失败时 {@code data} 为 {@code null}
     */
    @PostMapping("/ware/waresku/canDelete")
    R<List<SkuDeleteBlockerTo>> canDelete(@RequestBody List<Long> skuIds);
}
