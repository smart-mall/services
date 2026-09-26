package coupon.fegin;

import common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

import java.util.Map;
@FeignClient("product")
public interface ProductFeignService {
    @PostMapping(value = "/product/spuinfo/getSpuNames")
    R<Map<Long, String>> getSpuNames(@RequestBody List<Long> spuIds);

    @PostMapping(value = "/product/skuinfo/getSkuNames")
    R<Map<Long, String>> getSkuNames(@RequestBody List<Long> spuIds);
}
