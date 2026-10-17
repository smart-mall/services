package coupon.service;

import coupon.entity.SeckillSkuRelationEntity;

import java.util.List;

/**
 * 秒杀下架通知：把"哪些场次、哪些商品"告诉 seckill，由它摘掉自己的缓存。
 *
 * <p>实现必须失败关闭：通知不成功就抛异常，让调用方的删除事务回滚，不允许出现
 * "库里已经删了、前台还在卖"。
 */
public interface SeckillOfflineService {

    /**
     * 下架整个场次。
     *
     * @param sessionIds 场次 ID 列表，不能为 {@code null}，可以为空集合；为空时不调用远端
     * @throws common.exception.BaseException 通知 seckill 失败时抛出，调用方应让事务回滚
     */
    void offlineSessions(List<Long> sessionIds);

    /**
     * 下架场次内的指定商品。
     *
     * @param relations 待删除的关联记录，不能为 {@code null}，可以为空集合；为空时不调用远端
     * @throws common.exception.BaseException 通知 seckill 失败时抛出，调用方应让事务回滚
     */
    void offlineSkus(List<SeckillSkuRelationEntity> relations);
}
