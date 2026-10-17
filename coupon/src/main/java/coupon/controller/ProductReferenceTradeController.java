package coupon.controller;

import common.to.CategoryCouponBlockerTo;
import common.to.ProductDeleteCheckTo;
import common.to.ProductReferenceBlockerTo;
import common.utils.R;
import coupon.service.ProductReferenceService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 删除商品与分类前的引用核对接口，供 product 的删除守卫同步调用。
 *
 * <p>身份口径与 {@link CouponTradeController} 一致：路径不在 {@code /front} 下，调用方是服务而不是
 * 浏览器，product 走 {@code @FeignClient("coupon")} 直连、不经网关。经网关进来的请求里
 * {@code /coupon/trade/**} 会按管理端接口要求管理端凭证，普通会员触达不到。
 *
 * <p><b>本类只回答"能不能删"，不修改任何数据。</b>失效引用（已过使用期的券、已结束的场次留下的关联行）
 * 不在这里删 —— 远程删除不可回滚，一旦 product 随后因为别的原因拒绝删除，这边已经删掉的东西回不来。
 * 失效引用的清理挂在删除成功之后的 {@code product.deleted} 与 {@code category.deleted} 事件上。
 *
 * <p>本类无状态、线程安全：只持有一个构造器注入的 service。
 */
@RestController
@RequestMapping("coupon/trade/reference")
public class ProductReferenceTradeController {

    private final ProductReferenceService productReferenceService;

    /**
     * 注入商品引用核对服务。
     *
     * @param productReferenceService 商品引用核对服务
     */
    public ProductReferenceTradeController(ProductReferenceService productReferenceService) {
        this.productReferenceService = productReferenceService;
    }

    /**
     * 查这个商品还被哪些活着的营销配置引用。
     *
     * <p>只读、幂等、无副作用。已过使用期的券与已结束的场次不算引用，不会出现在结果里。
     *
     * @param check 待删除商品的 SPU 与 SKU 主键，不能为 {@code null}；两个列表都允许为空
     * @return 统一响应；{@code code} 为 0 时 {@code data} 是阻塞清单，两类都为空表示没有引用
     */
    @PostMapping("/can-delete")
    public R<ProductReferenceBlockerTo> canDelete(@RequestBody ProductDeleteCheckTo check) {
        return R.ok(productReferenceService.listActiveBlockers(check.getSpuIds(), check.getSkuIds()));
    }

    /**
     * 查这些分类还被哪些仍然有效的券纳入了适用范围。
     *
     * <p>只读、幂等、无副作用。已过使用期的券不算引用，不会出现在结果里。入参是整棵子树的
     * 分类 ID —— 删除连子分类一起删，而券的分类范围可以配在任意一层。
     *
     * @param catIds 待删除的分类 ID，含子树里的子分类，不能为 {@code null}；允许为空集合
     * @return 统一响应；{@code code} 为 0 时 {@code data} 是阻塞清单，空列表表示没有引用
     */
    @PostMapping("/can-delete-category")
    public R<List<CategoryCouponBlockerTo>> canDeleteCategory(@RequestBody List<Long> catIds) {
        return R.ok(productReferenceService.listActiveCategoryBlockers(catIds));
    }
}
