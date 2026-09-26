package seckill.service;

import common.vo.MemberResponseVo;
import seckill.to.SeckillSkuRedisTo;

import java.util.List;


/**
 * 秒杀服务：负责秒杀商品上架与抢购下单。
 *
 * <p>场次数据来自 coupon 服务，商品信息来自 product 服务；秒杀缓存、库存信号量、限购占位都放在 Redis 里，
 * 抢购成功后通过 MQ 让 order 服务异步建单。
 */
public interface SeckillService {

    /**
     * 上架最近三天需要参与的秒杀场次与商品。
     *
     * <p>重复调用不会覆盖已上架的数据：Redis 里已存在的场次与 SKU 会被跳过，
     * 已下发的随机码和已初始化的库存信号量保持原值。
     */
    void uploadSeckillSkuLatest3Days();

    /**
     * 返回当前正在进行的秒杀场次里的商品。
     *
     * <p>契约上<b>不返回 null</b>：没有场次、被 Sentinel 限流都返回空集合，
     * 调用方（首页秒杀栏、秒杀页）不用先判空再遍历。
     *
     * @return 当前场次的商品列表，可能为空集合，不会为 {@code null}
     */
    List<SeckillSkuRedisTo> getCurrentSeckillSkus();

    /**
     * 返回某个 SKU 的秒杀信息，给商品详情页用（product 通过 Feign 调）。
     *
     * <p>不在秒杀时间区间内时返回的对象里 {@code randomCode} 为 {@code null} —— 随机码就是“能不能抢”的凭证，
     * 没到点或者已经结束都不该下发。
     *
     * @param skuId 商品 SKU ID，不能为 {@code null}
     * @return SKU 的秒杀信息；该 SKU 不在任何秒杀场次中时返回 {@code null}
     */
    SeckillSkuRedisTo getSkuSeckillInfo(Long skuId);

    /**
     * 秒杀下单：校验 + 扣信号量 + 发 MQ 让 order 异步建单。
     *
     * <p>实现方必须保证：同一会员在同一场次对同一 SKU 只成功一次；任何一步校验失败都不扣减库存。
     *
     * @param user   当前登录会员（控制器从 {@code X-Member-Claims} 取），不接受前端传 memberId
     * @param killId 场次 ID 与 SKU ID 拼接的凭证，形如 {@code 4-45}
     * @param key    上架时生成的随机码
     * @param num    购买数量，必须为正数且不超过 Redis 里的每人限购数
     * @return 秒杀订单号；此时订单还没落库，由 order 消费 MQ 后才创建
     * @throws InterruptedException 等待库存信号量时线程被中断
     * @throws common.exception.BaseException 抢不到时按原因抛 18xxx（见 {@link common.exception.BaseCodeEnum}）
     */
    String kill(MemberResponseVo user, String killId, String key, Integer num) throws InterruptedException;
}
