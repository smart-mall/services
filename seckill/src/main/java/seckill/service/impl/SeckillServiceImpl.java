package seckill.service.impl;


import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import common.exception.BaseCodeEnum;
import common.exception.BaseException;
import common.mq.MqConstant;
import common.mq.MqPublisher;
import common.to.SeckillOfflineTo;
import common.to.mq.SeckillOrderTo;
import common.utils.R;
import common.vo.MemberResponseVo;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RSemaphore;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import seckill.feign.CouponFeignService;
import seckill.feign.ProductFeignService;
import seckill.service.SeckillService;
import seckill.to.SeckillSkuRedisTo;
import seckill.vo.SeckillSessionWithSkusVo;
import seckill.vo.SeckillUploadResultVo;
import seckill.vo.SkuInfoVo;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


/**
 * 秒杀服务实现：上架时把场次与 SKU 写进 Redis，抢购的校验与扣减也全在 Redis 上完成。
 *
 * <p>缓存布局：{@code seckill:sessions:开始时间_结束时间} 存场次的 killId 列表，{@code seckill:skus} 存
 * killId 到 {@link SeckillSkuRedisTo} JSON 的映射，{@code seckill:stock:随机码} 存库存信号量。
 */
@Slf4j
@Service
public class SeckillServiceImpl implements SeckillService {

    /** 秒杀场次、SKU 缓存与限购占位都放在这个 Redis 里。 */
    @Autowired
    private StringRedisTemplate redisTemplate;

    /** 秒杀场次的数据来源，本服务不保存活动表。 */
    @Autowired
    private CouponFeignService couponFeignService;

    /** 取 SKU 基本信息，上架时拼进缓存供前端展示。 */
    @Autowired
    private ProductFeignService productFeignService;

    /** 提供库存信号量与上架任务的分布式锁。 */
    @Autowired
    private RedissonClient redissonClient;

    /** 投递秒杀订单消息，订单由 order 服务异步落库。 */
    @Autowired
    private MqPublisher mqPublisher;

    /** 场次缓存 key 前缀，完整 key 为「前缀 + 开始时间戳_结束时间戳」，值是场次内所有 killId 的 list。 */
    private final String SESSION_CACHE_PREFIX = "seckill:sessions:";

    /** SKU 秒杀缓存，作为 hash 使用：field 是 killId，value 是 {@link SeckillSkuRedisTo} 的 JSON。 */
    private final String SECKILL_CACHE_PREFIX = "seckill:skus";

    /** 库存信号量 key 前缀，完整 key 为「前缀 + 商品随机码」，信号量的当前值就是剩余可抢数量。 */
    private final String SKU_STOCK_SEMAPHORE = "seckill:stock:";

    /** 上架锁 key：定时任务与后台手动触发共用，保证同一时刻只有一个实例在真正上架。 */
    private final String UPLOAD_LOCK = "seckill:upload:lock";

    /** {@inheritDoc} */
    @Override
    public SeckillUploadResultVo uploadSeckillSkuLatest3Days() {

        RLock lock = redissonClient.getLock(UPLOAD_LOCK);
        try {
            // 租期 10 秒：实例宕机后锁自动过期，不会让上架永久卡死
            lock.lock(10, TimeUnit.SECONDS);
            return uploadLatest3Days();
        } finally {
            // 没抢到锁时当前线程并不持有锁，直接 unlock 会抛异常并盖掉真实的失败原因
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 执行一次上架：取场次、写场次列表与商品缓存，并统计写入条数。
     *
     * @return 上架结果，时间窗口内没有场次时各项计数为 0
     */
    private SeckillUploadResultVo uploadLatest3Days() {

        SeckillUploadResultVo result = new SeckillUploadResultVo();

        R<List<SeckillSessionWithSkusVo>> lates3DaySession = couponFeignService.getLates3DaySession();
        // code 非 0 表示 coupon 查询失败，此时 data 不可用，本轮直接放弃上架
        if (lates3DaySession.getCode() != 0) {
            log.error("查询最近三天秒杀场次失败，本轮跳过上架：code={}", lates3DaySession.getCode());
            return result;
        }

        List<SeckillSessionWithSkusVo> sessionData = lates3DaySession.getData();
        // coupon 侧查不到场次时返回 null 而不是空集合，这里必须判空
        if (sessionData == null || sessionData.isEmpty()) {
            log.info("最近三天没有秒杀场次，本轮跳过上架");
            return result;
        }

        result.setSessionCount(sessionData.size());

        saveSessionInfos(sessionData);
        saveSessionSkuInfo(sessionData, result);

        log.info("秒杀上架完成：场次 {} 个，新写入商品 {} 条，跳过已存在 {} 条",
                result.getSessionCount(), result.getUploadedSkuCount(), result.getSkippedSkuCount());
        return result;
    }


    private void saveSessionInfos(List<SeckillSessionWithSkusVo> sessions) {

        sessions.forEach(session -> {

            long startTime = session.getStartTime().getTime();
            long endTime = session.getEndTime().getTime();

            // 起止时间戳写进 key：查当前场次时只靠 key 就能筛出进行中的那一个
            String key = SESSION_CACHE_PREFIX + startTime + "_" + endTime;

            // 必须判重：leftPushAll 是追加，重复上架会让同一个场次的 killId 越堆越多
            Boolean hasKey = redisTemplate.hasKey(key);
            if (!hasKey) {
                List<String> skuIds = session.getRelationSkus().stream().map(item ->
                        item.getPromotionSessionId() + "-" + item.getSkuId().toString())
                        .collect(Collectors.toList());
                redisTemplate.opsForList().leftPushAll(key,skuIds);
            }
        });

    }

    /**
     * 把场次关联的 SKU 秒杀信息写入 Redis hash，并按库存初始化信号量。
     *
     * @param sessions 场次及其商品，来自 coupon 服务
     * @param result   上架结果，用于累计本次新写入与跳过的条目数
     */
    private void saveSessionSkuInfo(List<SeckillSessionWithSkusVo> sessions, SeckillUploadResultVo result) {

        sessions.forEach(session -> {
            BoundHashOperations<String, Object, Object> ops = redisTemplate.boundHashOps(SECKILL_CACHE_PREFIX);
            session.getRelationSkus().forEach(seckillSkuVo -> {
                String token = UUID.randomUUID().toString().replace("-", "");
                String redisKey = seckillSkuVo.getPromotionSessionId().toString() + "-" + seckillSkuVo.getSkuId().toString();

                // 已上架的 SKU 整体跳过：重写会换掉随机码，让用户手里正在用的页面立刻失效
                if (Boolean.FALSE.equals(ops.hasKey(redisKey))) {
                    SeckillSkuRedisTo redisTo = new SeckillSkuRedisTo();
                    Long skuId = seckillSkuVo.getSkuId();

                    // 1. SKU 基本信息
                    R<SkuInfoVo> info = productFeignService.getSkuInfo(skuId);

                    if (info.getCode() == 0) {
                        SkuInfoVo skuInfo = info.getData();
                        redisTo.setSkuInfo(skuInfo);
                    }

                    // 2. 秒杀信息与时间区间
                    BeanUtils.copyProperties(seckillSkuVo,redisTo);

                    redisTo.setStartTime(session.getStartTime().getTime());
                    redisTo.setEndTime(session.getEndTime().getTime());

                    // 3. 随机码：抢购时必须原样带回，挡住直接拼 killId 的请求
                    redisTo.setRandomCode(token);

                    String seckillValue = JSON.toJSONString(redisTo);
                    String key = seckillSkuVo.getPromotionSessionId().toString() + "-" + seckillSkuVo.getSkuId();
                    ops.put(key,seckillValue);

                    // 4. 库存信号量：key 用随机码，许可数就是本场次参与秒杀的总量
                    RSemaphore semaphore = redissonClient.getSemaphore(SKU_STOCK_SEMAPHORE + token);
                    semaphore.trySetPermits(seckillSkuVo.getSeckillCount());

                    result.setUploadedSkuCount(result.getUploadedSkuCount() + 1);
                } else {
                    result.setSkippedSkuCount(result.getSkippedSkuCount() + 1);
                }
            });
        });
    }


    /** {@inheritDoc} */
    @SentinelResource(value = "getCurrentSeckillSkusResource",blockHandler = "blockHandler")
    @Override
    public List<SeckillSkuRedisTo> getCurrentSeckillSkus() {
        try (Entry entry = SphU.entry("seckillSkus")) {
            long currentTime = System.currentTimeMillis();

            // 1. 找出当前时间所在的场次
            Set<String> keys = redisTemplate.keys(SESSION_CACHE_PREFIX + "*");
            if (keys == null || keys.isEmpty()) {
                log.debug("Redis 里没有任何秒杀场次，返回空集合");
                return List.of();
            }
            for (String key : keys) {
                // key 形如 seckill:sessions:开始时间_结束时间；去掉前缀后剩下的就是 开始时间_结束时间
                String replace = key.replace(SESSION_CACHE_PREFIX, "");
                String[] s = replace.split("_");
                long startTime = Long.parseLong(s[0]);
                long endTime = Long.parseLong(s[1]);

                if (currentTime >= startTime && currentTime <= endTime) {
                    // 2. 取该场次的商品，一次最多 100 个 killId
                    List<String> range = redisTemplate.opsForList().range(key, -100, 100);
                    BoundHashOperations<String, String, String> hasOps = redisTemplate.boundHashOps(SECKILL_CACHE_PREFIX);
                    // assert 默认不生效，range 为 null 时下面的 multiGet 会抛 NPE
                    assert range != null;

                    List<String> listValue = hasOps.multiGet(range);
                    if (listValue != null) {
                        // 场次列表里可能残留已经下架的 skuId（hash 里那份没了），multiGet 对应位置就是 null，
                        // 直接 parse 会得到 null 元素，到了前端就是一堆空卡片。这里滤掉。
                        return listValue.stream()
                                .filter(Objects::nonNull)
                                .map(item -> JSON.parseObject((String) item, SeckillSkuRedisTo.class))
                                .filter(Objects::nonNull)
                                .collect(Collectors.toList());
                    }
                    // 命中进行中的场次即结束循环，一次只返回一个场次
                    break;
                }
            }
        } catch (BlockException e) {
            log.error("资源被限流{}",e.getMessage());
        }

        return List.of();
    }

    /**
     * Sentinel 限流后的兜底回调，返回空集合。
     *
     * <p>方法名必须与 {@code @SentinelResource} 上声明的 {@code blockHandler} 一致，
     * 否则限流时不会走到这里，而是直接把 {@code BlockException} 抛给调用方。
     *
     * @param e Sentinel 传入的限流异常
     * @return 空集合，保证调用方拿到的始终是可遍历的列表
     */
    public List<SeckillSkuRedisTo> blockHandler(BlockException e) {

        log.error("getCurrentSeckillSkusResource被限流了,{}",e.getMessage());
        return List.of();
    }


    /** {@inheritDoc} */
    @Override
    public SeckillSkuRedisTo getSkuSeckillInfo(Long skuId) {

        BoundHashOperations<String, String, String> hashOps = redisTemplate.boundHashOps(SECKILL_CACHE_PREFIX);

        // 1. 在 hash 里找这个 SKU 的 field
        Set<String> keys = hashOps.keys();
        if (keys != null && !keys.isEmpty()) {
            // 正则整体匹配 field：前面那截是场次 id，位数不定，用 \d 表示
            String reg = "\\d-" + skuId;
            for (String key : keys) {
                if (Pattern.matches(reg,key)) {
                    String redisValue = hashOps.get(key);
                    SeckillSkuRedisTo redisTo = JSON.parseObject(redisValue, SeckillSkuRedisTo.class);

                    // 2. 不在时间区间内时清掉随机码：它是抢购凭证，不能让详情页提前拿到
                    Long currentTime = System.currentTimeMillis();
                    Long startTime = redisTo.getStartTime();
                    Long endTime = redisTo.getEndTime();
                    if (currentTime >= startTime && currentTime <= endTime) {
                        return redisTo;
                    }
                    redisTo.setRandomCode(null);
                    return redisTo;
                }
            }
        }
        return null;
    }


    /** {@inheritDoc} */
    @Override
    public String kill(MemberResponseVo user, String killId, String key, Integer num) throws InterruptedException {

        long start = System.currentTimeMillis();
        Long memberId = user.getId();

        // 1. 取缓存里的秒杀信息：killId 就是 hash 的 field（场次id-skuId）
        BoundHashOperations<String, String, String> hashOps = redisTemplate.boundHashOps(SECKILL_CACHE_PREFIX);
        String skuInfoValue = hashOps.get(killId);
        if (!StringUtils.hasText(skuInfoValue)) {
            // 场次已经过期被清理，或者前端拼了一个不存在的 killId
            throw new BaseException(BaseCodeEnum.SECKILL_NOT_FOUND);
        }
        SeckillSkuRedisTo redisTo = JSON.parseObject(skuInfoValue, SeckillSkuRedisTo.class);
        Long startTime = redisTo.getStartTime();
        Long endTime = redisTo.getEndTime();
        long currentTime = System.currentTimeMillis();

        // 2. 时间合法性：活动区间之外一律不接
        if (currentTime < startTime || currentTime > endTime) {
            throw new BaseException(BaseCodeEnum.SECKILL_NOT_FOUND);
        }

        // 3. 随机码 + killId 双重校验：随机码只有秒杀进行中才随详情下发，
        //    所以它顺带挡住了拿一个活动开始前存下来的页面来重放。
        String randomCode = redisTo.getRandomCode();
        String skuId = redisTo.getPromotionSessionId() + "-" + redisTo.getSkuId();
        if (randomCode == null || !randomCode.equals(key) || !killId.equals(skuId)) {
            throw new BaseException(BaseCodeEnum.SECKILL_TOKEN_INVALID);
        }

        // 4. 限购：num 不能超过缓存里的每人限购数
        Integer seckillLimit = redisTo.getSeckillLimit();
        if (seckillLimit != null && num > seckillLimit) {
            throw new BaseException(BaseCodeEnum.SECKILL_LIMIT_EXCEEDED);
        }

        // 5. 库存：信号量的当前值就是剩余可抢数量（它本身就存在这个 key 上，直接读）。
        //    判断必须用 count < num：写成 count <= num 会让最后 num 件永远卖不出去。
        String seckillCount = redisTemplate.opsForValue().get(SKU_STOCK_SEMAPHORE + randomCode);
        int count = seckillCount == null ? 0 : Integer.parseInt(seckillCount);
        if (count < num) {
            throw new BaseException(BaseCodeEnum.SECKILL_SOLD_OUT);
        }

        // 6. 幂等：一个人在同一场次里只能抢一次这个商品。SETNX 原子占位，挡住连点和并发重复下单。
        String redisKey = memberId + "-" + skuId;
        Long ttl = endTime - currentTime;
        Boolean first = redisTemplate.opsForValue()
                .setIfAbsent(redisKey, num.toString(), ttl, TimeUnit.MILLISECONDS);
        if (!Boolean.TRUE.equals(first)) {
            throw new BaseException(BaseCodeEnum.SECKILL_ALREADY_BOUGHT);
        }

        // 7. 扣信号量
        RSemaphore semaphore = redissonClient.getSemaphore(SKU_STOCK_SEMAPHORE + randomCode);
        if (!semaphore.tryAcquire(num, 100, TimeUnit.MILLISECONDS)) {
            // 第 5 步读到的只是读的那一刻的值，并发下可能已经被别人抢走了。
            // 抢不到必须撤掉第 6 步的占位，否则这个人本场次再也抢不了。
            redisTemplate.delete(redisKey);
            throw new BaseException(BaseCodeEnum.SECKILL_SOLD_OUT);
        }

        // 8. 抢到了：生成订单号并发 MQ，由 order 异步建单。
        //    返回给前端的只是订单号，此刻订单还没落库，前端得轮询订单列表等它出现。
        String orderSn = IdWorker.getTimeId();
        SeckillOrderTo orderTo = new SeckillOrderTo();
        orderTo.setOrderSn(orderSn);
        orderTo.setMemberId(memberId);
        orderTo.setNum(num);
        orderTo.setPromotionSessionId(redisTo.getPromotionSessionId());
        orderTo.setSkuId(redisTo.getSkuId());
        orderTo.setSeckillPrice(redisTo.getSeckillPrice());
        mqPublisher.publish(
                MqConstant.Exchanges.ORDER,
                MqConstant.RoutingKeys.ORDER_SECKILL_CREATED,
                orderTo);

        log.info("秒杀成功，memberId={}，killId={}，num={}，orderSn={}，耗时={}ms",
                memberId, killId, num, orderSn, System.currentTimeMillis() - start);
        return orderSn;
    }

    /** {@inheritDoc} */
    @Override
    public int offline(List<SeckillOfflineTo> requests) {

        if (requests == null || requests.isEmpty()) {
            return 0;
        }

        BoundHashOperations<String, String, String> hashOps = redisTemplate.boundHashOps(SECKILL_CACHE_PREFIX);
        int removed = 0;

        for (SeckillOfflineTo request : requests) {
            Long sessionId = request.getPromotionSessionId();
            if (sessionId == null) {
                continue;
            }

            for (String killId : resolveKillIds(hashOps, sessionId, request.getSkuIds())) {
                // 库存计数键用随机码命名，随机码只存在这份缓存里，必须先读出来才能删掉它
                String cached = hashOps.get(killId);
                if (cached != null) {
                    SeckillSkuRedisTo redisTo = JSON.parseObject(cached, SeckillSkuRedisTo.class);
                    if (redisTo != null && redisTo.getRandomCode() != null) {
                        redisTemplate.delete(SKU_STOCK_SEMAPHORE + redisTo.getRandomCode());
                    }
                }

                hashOps.delete(killId);
                removeFromSessionLists(killId);
                removed++;
            }
        }

        log.info("秒杀下架完成：摘掉商品 {} 条", removed);
        return removed;
    }

    /**
     * 取该场次在缓存里的商品条目名。
     *
     * @param hashOps   秒杀商品缓存
     * @param sessionId 场次 ID
     * @param skuIds    指定要下架的 SKU；为空表示整个场次
     * @return 条目名列表，形如 {@code 场次id-skuId}
     */
    private List<String> resolveKillIds(BoundHashOperations<String, String, String> hashOps,
                                        Long sessionId, List<Long> skuIds) {

        if (skuIds != null && !skuIds.isEmpty()) {
            return skuIds.stream()
                    .filter(Objects::nonNull)
                    .map(skuId -> sessionId + "-" + skuId)
                    .toList();
        }

        Set<String> fields = hashOps.keys();
        if (fields == null || fields.isEmpty()) {
            return List.of();
        }

        // 按场次前缀扫：库里已经删掉的关联行在缓存里可能还留着，只按传参拼名字会漏掉它们
        String prefix = sessionId + "-";
        return fields.stream()
                .filter(field -> field != null && field.startsWith(prefix))
                .toList();
    }

    /**
     * 把已下架的商品编号从所有场次列表里摘掉，列表被摘空时连键一起删。
     *
     * @param killId 商品条目名，形如 {@code 场次id-skuId}
     */
    private void removeFromSessionLists(String killId) {

        Set<String> sessionKeys = redisTemplate.keys(SESSION_CACHE_PREFIX + "*");
        if (sessionKeys == null || sessionKeys.isEmpty()) {
            return;
        }

        for (String key : sessionKeys) {
            redisTemplate.opsForList().remove(key, 0, killId);
            Long size = redisTemplate.opsForList().size(key);
            if (size != null && size == 0) {
                redisTemplate.delete(key);
            }
        }
    }

}
