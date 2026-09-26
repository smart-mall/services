package product.feign;

import es.SkuEsModel;
import common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * search 服务的 Feign 客户端：商品上架时把 SKU 文档写入 Elasticsearch。
 */
@FeignClient("search")
public interface SearchFeignService {

    /**
     * 把上架商品的 SKU 文档批量写入 Elasticsearch。
     *
     * @param skuEsModels 上架商品的 SKU 文档列表，不能为 {@code null}
     * @return 统一响应体，{@code data} 恒为 {@code null}；全部写入成功时 {@code code} 为 0，存在写入
     *         失败时为 {@code 11000}（商品上架异常）
     */
    @PostMapping("/search/product/save")
    R<Void> productStatusUp(@RequestBody List<SkuEsModel> skuEsModels);
}
