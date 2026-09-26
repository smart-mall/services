package product.web;

import common.exception.BaseCodeEnum;
import common.utils.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import product.service.CategoryService;
import product.service.SkuInfoService;
import product.vo.CategoryVo;
import product.vo.SkuItemVo;

import java.util.List;
import java.util.concurrent.ExecutionException;
/**
 * 前台商品接口：商城首页/导航的三级分类树，与商品详情页所需的全部数据。
 *
 * <p>路径为 {@code product/front}，网关对 {@code /api/{模块}/front/**} 放行且不要求登录态，
 * 再由 {@code product-route}（{@code Path=/api/product/**} 并剥掉 {@code /api}）转发到本服务。
 *
 * <p>{@code {skuId}} 会把 {@code 1.json} 整段吃掉再转 Long，请求路径不要带 {@code .json}
 * 后缀，否则得到 400 而不是 404。
 */
@Slf4j
@RestController
@RequestMapping("product/front")
public class WebController {

    private final CategoryService categoryService;

    private final SkuInfoService skuInfoService;

    /**
     * 由 Spring 注入分类与 sku 服务，创建后即可直接调用。
     *
     * @param categoryService 分类服务
     * @param skuInfoService sku 服务
     */
    public WebController(CategoryService categoryService, SkuInfoService skuInfoService) {
        this.categoryService = categoryService;
        this.skuInfoService = skuInfoService;
    }

    /**
     * 返回首页与全局导航使用的完整三级分类树。
     *
     * <p>只含 {@code showStatus} 为 1 的分类，一级分类与各级子分类均按 {@code sort} 升序；
     * 结果带 {@code category} 缓存，分类的增删改会整体清除它。
     *
     * @return 一级分类列表，子分类通过 {@code children} 嵌套
     */
    @GetMapping("catalog")
    public R<List<CategoryVo>> catalogJson() {
        log.debug("查询首页三级分类树");
        return R.ok(categoryService.getCatalogTree());
    }

    /**
     * 返回商品详情页所需的全部数据：sku 基本信息、图集、销售属性、商品介绍、规格参数、秒杀优惠与是否有货。
     *
     * <p>基本信息之外的各数据块在线程池里并行加载：库存服务异常只记日志、保留默认「有货」，
     * 秒杀信息非 0 码按无秒杀处理。
     *
     * @param skuId sku ID
     * @return 商品详情；商品不存在时返回 {@code 11008}（商品不存在），{@code data} 为 {@code null}
     * @throws ExecutionException 并行加载任务执行失败时抛出，由全局异常处理器兜住
     * @throws InterruptedException 等待并行任务时当前线程被中断
     */
    @GetMapping("/item/{skuId}")
    public R<SkuItemVo> skuItem(@PathVariable("skuId") Long skuId) throws ExecutionException, InterruptedException {
        log.debug("查询商品详情，skuId={}", skuId);

        SkuItemVo item = skuInfoService.item(skuId);
        if (item.getInfo() == null) {
            log.warn("商品不存在，skuId={}", skuId);
            return R.error(BaseCodeEnum.PRODUCT_NOT_FOUND);
        }
        return R.ok(item);
    }
}
