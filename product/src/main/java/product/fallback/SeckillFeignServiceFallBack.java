package product.fallback;


import common.exception.BaseCodeEnum;
import common.utils.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import product.feign.SeckillFeignService;


import product.vo.SeckillSkuVo;
/**
 * seckill 秒杀查询的 Sentinel 熔断降级实现，由 {@link SeckillFeignService} 的
 * {@code @FeignClient(fallback = ...)} 挂载，seckill 不可用或调用被熔断时顶替真实请求。
 *
 * <p>固定返回 {@code 10003}（请求流量过大）且不带数据，而不是抛异常：调用方
 * {@code SkuInfoServiceImpl#item} 只在 code 为 0 时填充秒杀信息，因此降级表现为「该商品没有秒杀
 * 活动」，商品详情页其余数据照常返回。
 *
 * <p>无状态、线程安全。
 */
@Slf4j
@Component
public class SeckillFeignServiceFallBack implements SeckillFeignService {
    /**
     * 降级返回秒杀信息查询失败。
     *
     * @param skuId 要查询的 sku ID，降级分支不使用该参数
     * @return code 为 10003、data 为 {@code null} 的失败响应
     */
    @Override
    public R<SeckillSkuVo> getSkuSeckilInfo(Long skuId) {
        log.info("熔断方法调用：getSkuSeckilInfo");
        return R.error(BaseCodeEnum.TO_MANY_REQUEST);
    }
}
