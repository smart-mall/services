package seckill.web;

import common.utils.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import seckill.service.SeckillService;
import seckill.to.SeckillSkuRedisTo;

/**
 * 秒杀对内的接口，只有服务之间调，不面向浏览器。
 *
 * <p>{@code GET /sku/seckill/{skuId}} 是 product 渲染商品详情时通过 Feign 调的
 * （见 {@code product.feign.SeckillFeignService}），用来把秒杀价塞进 {@code SkuItemVo.seckillSkuVo}。
 * 它<b>必须</b>留在这里、也<b>必须</b>保持这个路径：改了就要连 product 一起改并重新部署，
 * 而它本身返回的就是 JSON，没有任何"要迁到 SPA"的成分。</p>
 *
 * <p>之所以从原来的 {@code SeckillController} 挪出来单独一个类，是因为那个类同时装着前台接口
 * 和返回视图的 {@code /kill}，整类删掉的时候这个接口没地方放了。
 * 路径不带模块前缀，走的是 Feign 直连（lb://seckill），不经过网关的 {@code /api/**} 路由。</p>
 */
@Slf4j
@RestController
public class SeckillInternalController {

    private final SeckillService seckillService;

    public SeckillInternalController(SeckillService seckillService) {
        this.seckillService = seckillService;
    }

    /**
     * 某个 sku 的秒杀信息。
     *
     * <p>不在秒杀时间区间内时返回的对象里 {@code randomCode} 为 null ——
     * 随机码是"能不能抢"的凭证，没到点不下发；已经结束的场次由调用方（product）自己置空。</p>
     */
    @GetMapping("/sku/seckill/{skuId}")
    public R getSkuSeckillInfo(@PathVariable("skuId") Long skuId) {
        log.debug("根据skuId查询商品是否参加秒杀活动:{}", skuId);

        SeckillSkuRedisTo to = seckillService.getSkuSeckillInfo(skuId);

        return R.ok().setData(to);
    }
}
