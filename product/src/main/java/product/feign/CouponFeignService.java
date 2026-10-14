package product.feign;

import common.to.SkuPromotionVo;
import common.to.SkuReductionTo;
import common.to.SpuBoundTo;
import common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * coupon 服务的 Feign 客户端：发布商品时写入积分与优惠信息。
 *
 * <p>返回值统一是 {@link R}，业务是否成功看 {@code code}，{@code data} 恒为 {@code null}。
 */
@FeignClient("coupon")
public interface CouponFeignService {

    /**
     * 保存 spu 的积分设置：购买该 spu 可获得的购物积分与成长积分。
     *
     * @param spuBoundTo 积分设置，{@code spuId} 必填，不能为 {@code null}
     * @return 统一响应体，{@code data} 恒为 {@code null}；非 0 的 {@code code} 表示下游写入失败
     */
    @PostMapping("/coupon/spubounds/save")
    R<Void> saveSpuBounds(@RequestBody SpuBoundTo spuBoundTo);

    /**
     * 保存 sku 的优惠信息，一次写入阶梯价、满减与会员价三张表。
     *
     * <p>下游三段数据各自判断：满件数大于 0 才写阶梯价，满金额大于 0 才写满减，会员价列表非空且
     * 价格大于 0 才写会员价。
     *
     * @param skuReductionTo sku 优惠信息，{@code skuId} 必填，不能为 {@code null}
     * @return 统一响应体，{@code data} 恒为 {@code null}；非 0 的 {@code code} 表示下游写入失败
     */
    @PostMapping("/coupon/skufullreduction/saveInfo")
    R<Void> saveSkuReduction(@RequestBody SkuReductionTo skuReductionTo);

    /**
     * 按 skuId 批量取商品促销规则：满减与阶梯价。
     *
     * <p>商品详情页展示用。回的是规则本身，命中哪一档由算价的一方决定 ——
     * 详情页只把"满多少减多少""满几件打几折"显示出来，不替会员算能减多少。
     *
     * @param skuIds SKU 标识集合，不能为 {@code null}，可以为空集合
     * @return 统一响应体；{@code code} 为 0 时 {@code data} 是配过促销的 SKU 的规则，
     *         两段都没配的 SKU 不在结果里；非 0 的 {@code code} 表示下游读取失败
     */
    @PostMapping("/coupon/trade/promotion/sku-rules")
    R<List<SkuPromotionVo>> skuRules(@RequestBody List<Long> skuIds);
}
