package search.controller;

import common.exception.BaseCodeEnum;
import common.to.es.SkuEsModel;
import common.utils.R;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import search.service.ProductSaveService;

import java.util.List;

@RequestMapping("/search/save")
@RestController
public class ElasticSaveController {
    @Autowired
    private ProductSaveService productSaveService;

    // 上架商品
    @PostMapping("/product")
    public R productStatusUp(@RequestBody List<SkuEsModel> models) {
        boolean b = productSaveService.productStatusUp(models);
        if (!b) {
            return R.error(BaseCodeEnum.PRODUCT_UP_EXCEPTION.getCode(), BaseCodeEnum.PRODUCT_UP_EXCEPTION.getMsg());
        } else {
            return R.ok();
        }
    }
}
