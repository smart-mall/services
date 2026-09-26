package product.feign;

import common.to.SkuReductionTo;
import common.to.SpuBoundTo;
import common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

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
}
