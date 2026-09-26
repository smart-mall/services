package product.feign;

import es.SkuEsModel;
import common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient("search")
public interface SearchFeignService {
    @PostMapping("/search/product/save")
    R<Void> productStatusUp(@RequestBody List<SkuEsModel> skuEsModels);
}
