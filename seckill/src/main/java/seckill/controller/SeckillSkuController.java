package seckill.controller;

import com.alibaba.fastjson.JSON;
import common.to.SeckillOfflineTo;
import common.utils.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import seckill.service.SeckillService;
import seckill.to.SeckillSkuRedisTo;

import java.util.List;

/**
 * 秒杀商品的服务间接口：供 product 渲染商品详情、供 coupon 删除场次或商品时下架。
 *
 * <p>路径不在 {@code /front} 下，且服务端口只在容器网络内暴露，调用方都是服务而非浏览器。
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

    /**
     * 下架指定场次或场次下的指定商品。
     *
     * <p>调用方（coupon）在删除场次或关联商品前同步调用本接口，并在同一个事务里等结果：
     * 摘不掉缓存就不能落库，否则会出现库里删了、前台还在卖。
     *
     * @param requests 下架请求，不能为 {@code null}；{@code skuIds} 为空表示下架整个场次
     * @return 统一响应体，数据为实际摘掉的商品条目数
     */
    @PostMapping("/offline")
    public R<Integer> offline(@RequestBody List<SeckillOfflineTo> requests) {
        log.info("秒杀下架：{}", JSON.toJSONString(requests));

        return R.ok(seckillService.offline(requests));
    }
}
