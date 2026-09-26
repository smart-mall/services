package seckill.controller;

import common.utils.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import seckill.service.SeckillService;
import seckill.to.SeckillSkuRedisTo;

/** 某个 sku 的秒杀信息，product 渲染商品详情时通过 Feign 调。 */
@Slf4j
@RestController
@RequestMapping("seckill/seckillsku")
public class SeckillSkuController {

    private final SeckillService seckillService;

    public SeckillSkuController(SeckillService seckillService) {
        this.seckillService = seckillService;
    }

    /** 不在秒杀时间区间内时 {@code randomCode} 为 null —— 那是"能不能抢"的凭证，没到点不下发 */
    @GetMapping("/info/{skuId}")
    public R<SeckillSkuRedisTo> getSkuSeckillInfo(@PathVariable("skuId") Long skuId) {
        log.debug("根据skuId查询商品是否参加秒杀活动:{}", skuId);

        SeckillSkuRedisTo to = seckillService.getSkuSeckillInfo(skuId);

        return R.ok(to);
    }
}
