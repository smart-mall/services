package product.feign;

import common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import product.fallback.SeckillFeignServiceFallBack;


import product.vo.SeckillSkuVo;
/**
 * seckill 服务的 Feign 客户端：查询 SKU 的秒杀活动信息。
 *
 * <p>已挂载 Sentinel 降级实现 {@link SeckillFeignServiceFallBack}：seckill 不可用或调用被熔断时
 * 返回 {@code 10003}（请求流量过大）且 {@code data} 为 {@code null}，不抛异常。
 */
@FeignClient(value = "seckill",fallback = SeckillFeignServiceFallBack.class)
public interface SeckillFeignService {

    /**
     * 查询指定 SKU 的秒杀信息。
     *
     * <p>不在秒杀时间区间内时 {@code randomCode} 为 {@code null} —— 那是「能不能抢」的凭证，没到点不下发。
     *
     * @param skuId 商品 SKU ID，不能为 {@code null}
     * @return 统一响应体，{@code data} 为该 SKU 的秒杀信息；该 SKU 未参加秒杀或调用被降级时
     *         {@code data} 为 {@code null}，调用方需按「没有秒杀活动」处理
     */
    @GetMapping(value = "/seckill/seckillsku/info/{skuId}")
    R<SeckillSkuVo> getSkuSeckilInfo(@PathVariable("skuId") Long skuId);

}
