package seckill.controller;

import common.utils.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import seckill.service.SeckillService;
import seckill.to.SeckillSkuRedisTo;

/**
 * 秒杀商品信息接口，供 product 服务渲染商品详情时通过 Feign 调用。
 */
@Slf4j
@RestController
@RequestMapping("seckill/seckillsku")
public class SeckillSkuController {

    private final SeckillService seckillService;

    public SeckillSkuController(SeckillService seckillService) {
        this.seckillService = seckillService;
    }

    /**
     * 返回指定 SKU 的秒杀信息。
     *
     * <p>不在秒杀时间区间内时 {@code randomCode} 为 {@code null} —— 那是“能不能抢”的凭证，没到点不下发。
     *
     * @param skuId 商品 SKU ID
     * @return 统一响应体，数据为该 SKU 的秒杀信息；该 SKU 未参加秒杀时数据为 {@code null}
     */
    @GetMapping("/info/{skuId}")
    public R<SeckillSkuRedisTo> getSkuSeckillInfo(@PathVariable("skuId") Long skuId) {
        log.debug("根据skuId查询商品是否参加秒杀活动:{}", skuId);

        SeckillSkuRedisTo to = seckillService.getSkuSeckillInfo(skuId);

        return R.ok(to);
    }
}
