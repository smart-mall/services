package seckill.service;

import seckill.to.SeckillSkuRedisTo;

import java.util.List;


public interface SeckillService {

    void uploadSeckillSkuLatest3Days();

    /**
     * 当前正在进行的秒杀场次里的商品。
     *
     * <p>契约上<b>不返回 null</b>：没有场次、被 Sentinel 限流都返回空集合，
     * 调用方（首页秒杀栏、秒杀页）不用先判空再遍历。</p>
     */
    List<SeckillSkuRedisTo> getCurrentSeckillSkus();

    /**
     * 某个 sku 的秒杀信息，给商品详情页用（product 通过 Feign 调）。
     *
     * <p>不在秒杀时间区间内时返回的对象里 {@code randomCode} 是 null —— 随机码就是"能不能抢"的凭证，
     * 没到点或者已经结束都不该下发。</p>
     */
    SeckillSkuRedisTo getSkuSeckillInfo(Long skuId);

    /**
     * 秒杀下单：校验 + 扣信号量 + 发 MQ 让 order 异步建单。
     *
     * <p>当前会员从 {@code LoginUserInterceptor} 写的 ThreadLocal 里取，和 order 模块同一套，
     * 不接受前端传 memberId。</p>
     *
     * @param killId 场次id-skuId
     * @param key    随机码
     * @param num    购买数量
     * @return 秒杀订单号。注意<b>订单此时还没落库</b>，是 order 消费 MQ 之后才建的
     * @throws common.exception.BaseException 抢不到时按原因抛 18xxx（见 {@link common.exception.BaseCodeEnum}）
     */
    String kill(String killId, String key, Integer num) throws InterruptedException;
}
