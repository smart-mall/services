package product.web;

import common.utils.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import product.service.CategoryService;
import product.service.SkuInfoService;
import product.vo.SkuItemVo;

import java.util.concurrent.ExecutionException;

/**
 * 前台（商城页面）接口，全部返回 JSON，给 Vue 单页应用调用。
 *
 * web 包下面原来有两个 controller：
 * 1、IndexController：GET /、/index.html 返回 Thymeleaf 视图 index；GET /index/catalog.json 返回分类 Map；
 * 2、ItemController ：GET /{skuId}.html 返回 Thymeleaf 视图 item。
 * 两者都是"服务端渲染前台页面"的写法：返回视图名、靠 Model 塞数据、路径对应 .html 页面，
 * 而且只有它们两个不带 product 前缀（因为它们靠网关的 Host 路由 gulimall.com / item.gulimall.com 转发）。
 * 前台换成 Vue 之后没有 Thymeleaf 和 Model 了，所以把这两个合成一个 JSON controller，路径统一带 product 前缀，
 * 这样就能被网关的 product-route（Path=/api/product/** 且剥掉 /api）命中：
 * GET /api/product/front/catalog       → /product/front/catalog
 * GET /api/product/front/item/{skuId}  → /product/front/item/{skuId}
 *
 * 注意路径不带 .json 后缀，前端不要再补：{skuId} 是贪婪匹配，item/1.json 会把 "1.json"
 * 整个吃掉再去转 Long，结果是 400 而不是 404。
 */
@Slf4j
@RestController
@RequestMapping("product/front")
public class WebController {

    private final CategoryService categoryService;

    private final SkuInfoService skuInfoService;

    public WebController(CategoryService categoryService, SkuInfoService skuInfoService) {
        this.categoryService = categoryService;
        this.skuInfoService = skuInfoService;
    }

    /**
     * 首页/全局导航使用的完整三级分类树
     */
    @GetMapping("catalog")
    public R catalogJson() {
        log.debug("查询首页三级分类树");
        return R.ok().setData(categoryService.getCatalogTree());
    }

    /**
     * 商品详情页所需的全部数据：基本信息、图片、销售属性、商品介绍、规格参数、秒杀优惠
     */
    @GetMapping("/item/{skuId}")
    public R skuItem(@PathVariable("skuId") Long skuId) throws ExecutionException, InterruptedException {
        log.debug("查询商品详情，skuId={}", skuId);

        SkuItemVo item = skuInfoService.item(skuId);
        if (item.getInfo() == null) {
            log.warn("商品不存在，skuId={}", skuId);
            return R.error("商品不存在");
        }
        return R.ok().setData(item);
    }
}
