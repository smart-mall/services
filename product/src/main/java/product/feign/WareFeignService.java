package product.feign;

import common.to.SkuDeleteBlockerTo;
import common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import product.vo.SkuHasStockVo;

import java.util.List;

@FeignClient("ware")
public interface WareFeignService {
    @PostMapping("/ware/waresku/hasstock")
    R<List<SkuHasStockVo>> getSkusHasStock(@RequestBody List<Long> skuIds);

    /**
     * 这些 sku 在仓库侧还能不能删。返回的 data 是阻塞清单，空集合表示都能删。
     */
    @PostMapping("/ware/waresku/canDelete")
    R<List<SkuDeleteBlockerTo>> canDelete(@RequestBody List<Long> skuIds);
}
