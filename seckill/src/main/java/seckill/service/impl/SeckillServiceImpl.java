package seckill.service.impl;


import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import common.exception.BaseCodeEnum;
import common.exception.BaseException;
import common.to.mq.SeckillOrderTo;
import common.utils.R;
import common.vo.MemberResponseVo;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RSemaphore;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import seckill.feign.CouponFeignService;
import seckill.feign.ProductFeignService;
import seckill.interceptor.LoginUserInterceptor;
import seckill.service.SeckillService;
import seckill.to.SeckillSkuRedisTo;
import seckill.vo.SeckillSessionWithSkusVo;
import seckill.vo.SkuInfoVo;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


@Slf4j
@Service
public class SeckillServiceImpl implements SeckillService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private CouponFeignService couponFeignService;

    @Autowired
    private ProductFeignService productFeignService;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    /**
     * 活动缓存前缀
     */
    private final String SESSION_CACHE_PREFIX = "seckill:sessions:";

    /**
     * 商品秒杀缓存
     */
    private final String SECKILL_CACHE_PREFIX = "seckill:skus";

    /**
     * 商品库存信号量
     */
    private final String SKU_STOCK_SEMAPHORE = "seckill:stock:";    //+商品随机码

    @Override
    public void uploadSeckillSkuLatest3Days() {

        //1、扫描最近三天的商品需要参加秒杀的活动
        R lates3DaySession = couponFeignService.getLates3DaySession();
        if (lates3DaySession.getCode() == 0) {
            //上架商品
            List<SeckillSessionWithSkusVo> sessionData = lates3DaySession.getData(
                    "data",
                    new TypeReference<>() {
                    });

            //缓存到Redis
            //1、缓存活动信息
            saveSessionInfos(sessionData);

            //2、缓存活动的关联商品信息
            saveSessionSkuInfo(sessionData);
        }

    }


    private void saveSessionInfos(List<SeckillSessionWithSkusVo> sessions) {

        sessions.forEach(session -> {

            //获取当前活动的开始和结束时间的时间戳
            long startTime = session.getStartTime().getTime();
            long endTime = session.getEndTime().getTime();

            //缓存存入到Redis中的key  当前活动的key
            String key = SESSION_CACHE_PREFIX + startTime + "_" + endTime;

            //判断Redis中是否有该信息，如果没有才进行添加
            Boolean hasKey = redisTemplate.hasKey(key);
            //缓存活动信息
            if (!hasKey) {
                //获取到活动中所有商品的skuId  场次id+商品id
                List<String> skuIds = session.getRelationSkus().stream().map(item ->
                        item.getPromotionSessionId() + "-" + item.getSkuId().toString())
                        .collect(Collectors.toList());
                redisTemplate.opsForList().leftPushAll(key,skuIds);
            }
        });

    }

    /**
     * 缓存秒杀活动所关联的[商品]信息
     */
    private void saveSessionSkuInfo(List<SeckillSessionWithSkusVo> sessions) {

        sessions.forEach(session -> {
            //准备hash操作，绑定hash
            BoundHashOperations<String, Object, Object> ops = redisTemplate.boundHashOps(SECKILL_CACHE_PREFIX);
            session.getRelationSkus().forEach(seckillSkuVo -> {
                //生成随机码
                String token = UUID.randomUUID().toString().replace("-", "");
                String redisKey = seckillSkuVo.getPromotionSessionId().toString() + "-" + seckillSkuVo.getSkuId().toString();

                if (Boolean.FALSE.equals(ops.hasKey(redisKey))) {
                    //缓存我们商品信息
                    SeckillSkuRedisTo redisTo = new SeckillSkuRedisTo();
                    Long skuId = seckillSkuVo.getSkuId();

                    //1、先查询sku的基本信息，调用远程服务
                    R info = productFeignService.getSkuInfo(skuId);

                    if (info.getCode() == 0) {
                        SkuInfoVo skuInfo = info.getData("skuInfo",new TypeReference<SkuInfoVo>(){});
                        redisTo.setSkuInfo(skuInfo);
                    }

                    //2、sku的秒杀信息
                    BeanUtils.copyProperties(seckillSkuVo,redisTo);

                    //3、设置当前商品的秒杀时间信息
                    redisTo.setStartTime(session.getStartTime().getTime());
                    redisTo.setEndTime(session.getEndTime().getTime());

                    //4、设置商品的随机码（防止恶意攻击）  UUID随机生成
                    redisTo.setRandomCode(token);

                    //序列化json格式存入Redis中
                    String seckillValue = JSON.toJSONString(redisTo);
//                    场次id + 商品id
                    String key = seckillSkuVo.getPromotionSessionId().toString() + "-" + seckillSkuVo.getSkuId();
//                    存入redis
                    ops.put(key,seckillValue);

                    //5、使用库存作为分布式Redisson信号量（限流）
                    //如果当前这个场次的商品库存信息已经上架就不需要上架
                    // 使用库存作为分布式信号量(库存数量)
//                    商品信号量 = 信号量前缀 + 商品UUID
                    RSemaphore semaphore = redissonClient.getSemaphore(SKU_STOCK_SEMAPHORE + token);
                    // 商品可以秒杀的数量作为信号量
                    semaphore.trySetPermits(seckillSkuVo.getSeckillCount());
                }
            });
        });
    }


    /**
     * 获取到当前可以参加秒杀商品的信息
     * @return
     */
    @SentinelResource(value = "getCurrentSeckillSkusResource",blockHandler = "blockHandler") // 自定义受保护的资源
    @Override
    public List<SeckillSkuRedisTo> getCurrentSeckillSkus() {
//        自定义受保护的资源
        try (Entry entry = SphU.entry("seckillSkus")) {
            //1、确定当前属于哪个秒杀场次
            long currentTime = System.currentTimeMillis();

            //从Redis中查询到所有key以seckill:sessions开头的所有数据
            Set<String> keys = redisTemplate.keys(SESSION_CACHE_PREFIX + "*");
            if (keys == null || keys.isEmpty()) {
                log.debug("Redis 里没有任何秒杀场次，返回空集合");
                return List.of();
            }
            for (String key : keys) {
                //seckill:sessions:1594396764000_1594453242000
                String replace = key.replace(SESSION_CACHE_PREFIX, "");  //1594396764000_1594453242000
                String[] s = replace.split("_");
                //获取存入Redis商品的开始时间
                long startTime = Long.parseLong(s[0]);
                //获取存入Redis商品的结束时间
                long endTime = Long.parseLong(s[1]);

                //判断是否是当前秒杀场次
                if (currentTime >= startTime && currentTime <= endTime) {
                    //2、获取这个秒杀场次需要的所有商品信息
                    List<String> range = redisTemplate.opsForList().range(key, -100, 100);
                    BoundHashOperations<String, String, String> hasOps = redisTemplate.boundHashOps(SECKILL_CACHE_PREFIX);
                    assert range != null;

//                    批量获取数据
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
                    break;
                }
            }
        } catch (BlockException e) {
            log.error("资源被限流{}",e.getMessage());
        }

        return List.of();
    }

    /**
     * 限流回调方法
     * @param e
     * @return
     */
    public List<SeckillSkuRedisTo> blockHandler(BlockException e) {

        log.error("getCurrentSeckillSkusResource被限流了,{}",e.getMessage());
        return List.of();
    }


    @Override
    public SeckillSkuRedisTo getSkuSeckillInfo(Long skuId) {

        //1、找到所有需要秒杀的商品的key信息---seckill:skus
        BoundHashOperations<String, String, String> hashOps = redisTemplate.boundHashOps(SECKILL_CACHE_PREFIX);

        //拿到所有的key
        Set<String> keys = hashOps.keys();
        if (keys != null && !keys.isEmpty()) {
            //4-45 正则表达式进行匹配
            String reg = "\\d-" + skuId;
            for (String key : keys) {
                //如果匹配上了
                if (Pattern.matches(reg,key)) {
                    //从Redis中取出数据来
                    String redisValue = hashOps.get(key);
                    //进行序列化
                    SeckillSkuRedisTo redisTo = JSON.parseObject(redisValue, SeckillSkuRedisTo.class);

                    //随机码
                    Long currentTime = System.currentTimeMillis();
                    Long startTime = redisTo.getStartTime();
                    Long endTime = redisTo.getEndTime();
                    //如果当前时间大于等于秒杀活动开始时间并且要小于活动结束时间
                    if (currentTime >= startTime && currentTime <= endTime) {
                        return redisTo;
                    }
                    redisTo.setRandomCode(null); // 非秒杀时间,删除请求随机码
                    return redisTo;
                }
            }
        }
        return null;
    }


    /**
     * 当前登录会员 id。
     *
     * <p>正常不可达的空分支：{@code LoginUserInterceptor} 已经把 {@code /seckill/front/kill}
     * 的匿名情况拦成 401 了。留着是因为这里依赖拦截器的路径配置，配置被改掉时要给出"没登录"而不是一个 NPE。</p>
     */
    private Long currentMemberId() {
        MemberResponseVo user = LoginUserInterceptor.loginUser.get();
        if (user == null || user.getId() == null) {
            throw new BaseException(BaseCodeEnum.NOT_LOGIN_EXCEPTION);
        }
        return user.getId();
    }

    @Override
    public String kill(String killId, String key, Integer num) throws InterruptedException {

        long start = System.currentTimeMillis();
        Long memberId = currentMemberId();

        //1、从 Redis 里取这次秒杀的商品信息。killId 就是那个 hash 的 field（场次id-skuId）
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

        //2、时间合法性：活动区间之外一律不接
        if (currentTime < startTime || currentTime > endTime) {
            throw new BaseException(BaseCodeEnum.SECKILL_NOT_FOUND);
        }

        //3、随机码 + killId 双重校验。随机码只有秒杀进行中才随详情下发，
        //   所以它顺带挡住了"拿一个活动开始前存下来的页面来重放"。
        String randomCode = redisTo.getRandomCode();
        String skuId = redisTo.getPromotionSessionId() + "-" + redisTo.getSkuId();
        if (randomCode == null || !randomCode.equals(key) || !killId.equals(skuId)) {
            throw new BaseException(BaseCodeEnum.SECKILL_TOKEN_INVALID);
        }

        //4、限购
        Integer seckillLimit = redisTo.getSeckillLimit();
        if (seckillLimit != null && num > seckillLimit) {
            throw new BaseException(BaseCodeEnum.SECKILL_LIMIT_EXCEEDED);
        }

        //5、库存：Redisson 信号量的当前值就是剩余可抢数量（它本身就存在这个 key 上，直接读）。
        //   必须是 >= num：老代码写的是 count > num，等于最后 num 件永远卖不出去。
        String seckillCount = redisTemplate.opsForValue().get(SKU_STOCK_SEMAPHORE + randomCode);
        int count = seckillCount == null ? 0 : Integer.parseInt(seckillCount);
        if (count < num) {
            throw new BaseException(BaseCodeEnum.SECKILL_SOLD_OUT);
        }

        //6、幂等：一个人在同一场次里只能抢一次这个商品。SETNX 原子占位，挡住连点和并发重复下单。
        String redisKey = memberId + "-" + skuId;
        Long ttl = endTime - currentTime;
        Boolean first = redisTemplate.opsForValue()
                .setIfAbsent(redisKey, num.toString(), ttl, TimeUnit.MILLISECONDS);
        if (!Boolean.TRUE.equals(first)) {
            throw new BaseException(BaseCodeEnum.SECKILL_ALREADY_BOUGHT);
        }

        //7、扣信号量
        RSemaphore semaphore = redissonClient.getSemaphore(SKU_STOCK_SEMAPHORE + randomCode);
        if (!semaphore.tryAcquire(num, 100, TimeUnit.MILLISECONDS)) {
            // 第 5 步读到的只是"读的那一刻"的值，并发下可能已经被别人抢走了。
            // 这里要把第 6 步的占位撤掉，否则这个人本场次再也抢不了 —— 老代码就漏了这一句。
            redisTemplate.delete(redisKey);
            throw new BaseException(BaseCodeEnum.SECKILL_SOLD_OUT);
        }

        //8、抢到了：生成订单号并发 MQ，由 order 异步建单。
        //   返回给前端的只是订单号，此刻订单还没落库，前端得轮询订单列表等它出现。
        String orderSn = IdWorker.getTimeId();
        SeckillOrderTo orderTo = new SeckillOrderTo();
        orderTo.setOrderSn(orderSn);
        orderTo.setMemberId(memberId);
        orderTo.setNum(num);
        orderTo.setPromotionSessionId(redisTo.getPromotionSessionId());
        orderTo.setSkuId(redisTo.getSkuId());
        orderTo.setSeckillPrice(redisTo.getSeckillPrice());
        rabbitTemplate.convertAndSend(
                "order-event-exchange",
                "order.seckill.order",
                orderTo);

        log.info("秒杀成功，memberId={}，killId={}，num={}，orderSn={}，耗时={}ms",
                memberId, killId, num, orderSn, System.currentTimeMillis() - start);
        return orderSn;
    }

}
