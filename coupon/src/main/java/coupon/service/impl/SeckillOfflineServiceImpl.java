package coupon.service.impl;

import common.exception.BaseCodeEnum;
import common.exception.BaseException;
import common.to.SeckillOfflineTo;
import common.utils.R;
import coupon.entity.SeckillSkuRelationEntity;
import coupon.feign.SeckillFeignService;
import coupon.service.SeckillOfflineService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 秒杀下架通知实现：把待下架的场次与商品转成 seckill 的契约，同步调用它摘缓存。
 *
 * <p>无状态，线程安全。调用失败或返回非 0 一律抛 {@link BaseException}，由调用方回滚删除。
 */
@Slf4j
@Service
public class SeckillOfflineServiceImpl implements SeckillOfflineService {

    private final SeckillFeignService seckillFeignService;

    /**
     * @param seckillFeignService 秒杀服务的 Feign 客户端，由容器注入
     */
    public SeckillOfflineServiceImpl(SeckillFeignService seckillFeignService) {
        this.seckillFeignService = seckillFeignService;
    }

    /** {@inheritDoc} */
    @Override
    public void offlineSessions(List<Long> sessionIds) {
        if (sessionIds == null || sessionIds.isEmpty()) {
            return;
        }

        List<SeckillOfflineTo> requests = sessionIds.stream()
                .filter(Objects::nonNull)
                .map(sessionId -> {
                    SeckillOfflineTo to = new SeckillOfflineTo();
                    to.setPromotionSessionId(sessionId);
                    return to;
                })
                .toList();

        call(requests);
    }

    /** {@inheritDoc} */
    @Override
    public void offlineSkus(List<SeckillSkuRelationEntity> relations) {
        if (relations == null || relations.isEmpty()) {
            return;
        }

        // 同一场次的多条关联合并成一个请求，减少一次删除发出的远端调用次数
        Map<Long, List<Long>> skuIdsBySession = relations.stream()
                .filter(relation -> relation.getPromotionSessionId() != null && relation.getSkuId() != null)
                .collect(Collectors.groupingBy(SeckillSkuRelationEntity::getPromotionSessionId,
                        Collectors.mapping(SeckillSkuRelationEntity::getSkuId, Collectors.toList())));
        if (skuIdsBySession.isEmpty()) {
            return;
        }

        List<SeckillOfflineTo> requests = skuIdsBySession.entrySet().stream()
                .map(entry -> {
                    SeckillOfflineTo to = new SeckillOfflineTo();
                    to.setPromotionSessionId(entry.getKey());
                    to.setSkuIds(entry.getValue());
                    return to;
                })
                .toList();

        call(requests);
    }

    /**
     * 调 seckill 下架；网络失败与返回非 0 都按失败处理。
     *
     * @param requests 下架请求，调用方保证非空
     * @throws BaseException 通知失败时抛出
     */
    private void call(List<SeckillOfflineTo> requests) {
        R<Integer> response;
        try {
            response = seckillFeignService.offline(requests);
        } catch (Exception e) {
            log.error("秒杀下架通知调用失败", e);
            throw new BaseException(BaseCodeEnum.SECKILL_OFFLINE_FAILED);
        }

        if (response == null || response.getCode() != 0) {
            log.error("秒杀下架通知返回失败：{}", response == null ? "无响应" : response.getCode());
            throw new BaseException(BaseCodeEnum.SECKILL_OFFLINE_FAILED);
        }

        log.info("秒杀下架通知完成：{} 个场次请求，远端摘掉 {} 条", requests.size(), response.getData());
    }
}
