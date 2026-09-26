package search.controller;

import common.exception.BaseCodeEnum;
import es.SkuEsModel;
import common.utils.R;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import search.service.ProductSaveService;

import java.util.List;

/**
 * 商品上架的 ES 写入接口：接收 SKU 文档列表并批量写入索引。
 */
@RequestMapping("/search/product")
@RestController
public class ElasticSaveController {
    @Autowired
    private ProductSaveService productSaveService;

    /**
     * 接收上架商品的 SKU 文档并批量写入 ES。
     *
     * @param models 上架商品的 SKU 文档列表
     * @return 全部写入成功时返回成功响应；存在失败时返回 {@link BaseCodeEnum#PRODUCT_UP_EXCEPTION} 对应的错误
     */
    @PostMapping("/save")
    public R<Void> productStatusUp(@RequestBody List<SkuEsModel> models) {
        boolean b = productSaveService.productStatusUp(models);
        if (!b) {
            return R.error(BaseCodeEnum.PRODUCT_UP_EXCEPTION);
        } else {
            return R.ok();
        }
    }
}
