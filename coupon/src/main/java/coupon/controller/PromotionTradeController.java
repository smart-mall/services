package coupon.controller;

import common.to.SkuPromotionVo;
import common.to.SpuBoundsVo;
import common.utils.R;
import coupon.service.SkuFullReductionService;
import coupon.service.SpuBoundsService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 促销与权益规则的只读服务间接口，供 order 算价时取规则。
 *
 * <p>身份口径与 {@link CouponTradeController} 完全一致：路径不在 {@code /front} 下，调用方是服务，
 * order 走 {@code @FeignClient("coupon")} 直连、不经网关。经网关进来的请求里
 * {@code /coupon/trade/**} 会按管理端接口要求管理端凭证，普通会员触达不到。
 *
 * <p><b>本类只给规则、不算钱。</b>券那一轮已经定过这条边界：规则的持有方声明"有哪些规则"，
 * 怎么并进金额由算价的一方做。积分设置同理 —— 这里只回"这个 SPU 配了多少"，
 * 按件相乘、向下取整、与基础赠送量怎么相加都在 order 侧。
 *
 * <p>本类无状态、线程安全：只持有一个构造器注入的 service。
 */
@RestController
@RequestMapping("coupon/trade/promotion")
public class PromotionTradeController {

    private final SpuBoundsService spuBoundsService;

    private final SkuFullReductionService skuFullReductionService;

    /**
     * 注入积分设置服务与商品满减服务。
     *
     * @param spuBoundsService 商品 SPU 积分设置服务
     * @param skuFullReductionService 商品满减与阶梯价服务
     */
    public PromotionTradeController(SpuBoundsService spuBoundsService,
                                    SkuFullReductionService skuFullReductionService) {
        this.spuBoundsService = spuBoundsService;
        this.skuFullReductionService = skuFullReductionService;
    }

    /**
     * 按 spuId 批量查积分设置。
     *
     * <p>下单时由 order 调用：它手里只有购物项上的 spuId，没有这张表的行 id，
     * 也不读 coupon 的表。请求体是裸 {@code List} —— 只收一个 id 列表，再包一层对象没有信息量。
     *
     * @param spuIds 商品 SPU 标识集合，可以为空集合
     * @return 统一响应；{@code code} 为 0 时 {@code data} 是配置过的 SPU 的积分设置，
     *         没配置的 SPU 不在结果里；{@code code} 非 0 时 {@code data} 为 {@code null}
     */
    @PostMapping("/spu-bounds")
    public R<List<SpuBoundsVo>> spuBounds(@RequestBody List<Long> spuIds) {
        return R.ok(spuBoundsService.listBySpuIds(spuIds));
    }

    /**
     * 按 skuId 批量查促销规则：满减与阶梯价。
     *
     * <p>下单算价与商品详情页展示都用它。回的是规则本身 —— 命中哪一档、折后单价怎么算、
     * 与满减怎么先后作用，都由调用方按自己的金额决定。
     *
     * @param skuIds SKU 标识集合，可以为空集合
     * @return 统一响应；{@code code} 为 0 时 {@code data} 是配过促销的 SKU 的规则，
     *         两段都没配的 SKU 不在结果里；{@code code} 非 0 时 {@code data} 为 {@code null}
     */
    @PostMapping("/sku-rules")
    public R<List<SkuPromotionVo>> skuRules(@RequestBody List<Long> skuIds) {
        return R.ok(skuFullReductionService.listBySkuIds(skuIds));
    }

}
