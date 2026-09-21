package cart.service.impl;

import cart.feign.ProductFeignService;
import cart.interceptor.LoginUserInterceptor;
import cart.service.CartService;
import cart.vo.CartItemVo;
import cart.vo.CartVo;
import cart.vo.SkuInfoVo;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import common.exception.BaseCodeEnum;
import common.exception.BaseException;
import common.utils.R;
import common.vo.MemberResponseVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

import static common.constant.CartConstant.CART_PREFIX;


/**
 * 购物车实现。
 *
 * <p>存储是 Redis Hash：key = {@code gulimall:cart:<userId>}，field = skuId，value = CartItemVo 的 JSON。
 * 用 Hash 而不是一个 JSON 数组，是为了改一项只写一项，不用读改写整个车。</p>
 */
@Slf4j
@Service("cartService")
public class CartServiceImpl implements CartService {

    private final StringRedisTemplate redisTemplate;

    private final ProductFeignService productFeignService;

    private final ThreadPoolExecutor executor;

    public CartServiceImpl(StringRedisTemplate redisTemplate,
                           ProductFeignService productFeignService,
                           ThreadPoolExecutor executor) {
        this.redisTemplate = redisTemplate;
        this.productFeignService = productFeignService;
        this.executor = executor;
    }

    @Override
    public CartItemVo addToCart(Long skuId, Integer num) {
        BoundHashOperations<String, Object, Object> cartOps = cartOps();

        String cached = (String) cartOps.get(skuId.toString());
        if (cached != null) {
            // 车里已经有这个 sku：只累加数量，标题/图片/属性沿用上次查到的。
            // 商品改过名也无所谓，读购物车时会用最新数据刷新（见 refreshPrices）
            CartItemVo item = JSON.parseObject(cached, CartItemVo.class);
            item.setCount(item.getCount() + num);
            cartOps.put(skuId.toString(), JSON.toJSONString(item));
            return item;
        }

        CartItemVo item = new CartItemVo();

        // 两次远程调用互不依赖，并行发
        CompletableFuture<Void> skuInfoFuture = CompletableFuture.runAsync(() -> {
            R productSkuInfo = productFeignService.getInfo(skuId);
            SkuInfoVo skuInfo = productSkuInfo.getData("skuInfo", new TypeReference<SkuInfoVo>() {});
            if (skuInfo == null) {
                // 商品服务对不存在的 skuId 返回的是 {code:0, skuInfo:null}。
                // 不判空的话下面 setTitle 拿到 null，会往车里塞一条标题为空的幽灵商品，
                // 而且它在购物车页面上看起来"就是有点怪"，不会报任何错
                throw new BaseException(BaseCodeEnum.CART_SKU_NOT_FOUND, "商品不存在或已下架：" + skuId);
            }
            item.setSkuId(skuInfo.getSkuId());
            item.setTitle(skuInfo.getSkuTitle());
            item.setImage(skuInfo.getSkuDefaultImg());
            item.setPrice(skuInfo.getPrice());
            item.setCount(num);
        }, executor);

        CompletableFuture<Void> skuAttrValuesFuture = CompletableFuture.runAsync(
                () -> item.setSkuAttrValues(productFeignService.getSkuSaleAttrValues(skuId)), executor);

        // 原实现把 ExecutionException / InterruptedException 直接往外抛，Controller 也跟着 throws。
        // 那两个异常既没有 @ExceptionHandler 接，也不在 GlobalExceptionHandler 的覆盖范围里，
        // 最后落到 Spring 默认的错误页（没有 code / msg，前端只能提示一个取不到原因的失败）。
        // 在这里收口，转成 BaseException 走统一的 {code, msg} 格式。
        try {
            CompletableFuture.allOf(skuInfoFuture, skuAttrValuesFuture).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BaseException("加入购物车被中断，请重试");
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof BaseException baseException) {
                // 异步块里抛的 BaseException 被 CompletableFuture 包了一层，拆出来保住原始 code
                throw baseException;
            }
            log.error("加购时查询商品信息失败，skuId={}", skuId, cause);
            throw new BaseException("查询商品信息失败，请稍后重试");
        }

        cartOps.put(skuId.toString(), JSON.toJSONString(item));
        return item;
    }

    @Override
    public CartVo getCart() {
        List<CartItemVo> items = allItems();
        refreshPrices(items);

        CartVo cartVo = new CartVo();
        cartVo.setItems(items);
        return cartVo;
    }

    @Override
    public List<CartItemVo> getCheckedCartItems() {
        List<CartItemVo> checked = allItems().stream()
                .filter(item -> Boolean.TRUE.equals(item.getCheck()))
                .collect(Collectors.toList());
        refreshPrices(checked);
        return checked;
    }

    @Override
    public void checkItems(List<Long> skuIds, Boolean checked) {
        if (skuIds == null || skuIds.isEmpty()) {
            return;
        }
        BoundHashOperations<String, Object, Object> cartOps = cartOps();
        for (Long skuId : skuIds) {
            CartItemVo item = readItem(cartOps, skuId);
            if (item == null) {
                // 不报错。批量勾选时前端的列表可能已经过期（另一个标签页删掉了商品），
                // 为了一个失效的 skuId 让整次"全选"失败，用户会以为是勾选功能坏了
                log.debug("勾选时跳过购物车中不存在的商品：skuId={}", skuId);
                continue;
            }
            item.setCheck(checked);
            cartOps.put(skuId.toString(), JSON.toJSONString(item));
        }
    }

    @Override
    public void changeItemCount(Long skuId, Integer num) {
        BoundHashOperations<String, Object, Object> cartOps = cartOps();
        CartItemVo item = readItem(cartOps, skuId);
        if (item == null) {
            // 和勾选不一样：改数量是"把这个 sku 的数量设成 N"，车里没有这个 sku
            // 说明前端的状态已经错了（或者商品已被别的标签页删掉），得让它知道
            throw new BaseException(BaseCodeEnum.CART_ITEM_NOT_FOUND);
        }
        item.setCount(num);
        cartOps.put(skuId.toString(), JSON.toJSONString(item));
    }

    @Override
    public void deleteCartItems(List<Long> skuIds) {
        if (skuIds == null || skuIds.isEmpty()) {
            return;
        }
        Object[] fields = skuIds.stream().map(String::valueOf).toArray();
        cartOps().delete(fields);
    }

    /**
     * 当前用户的购物车 Redis Hash。key 一定是 {@code gulimall:cart:<userId>} ——
     * 未登录的请求在 LoginUserInterceptor 那层就被 401 挡掉了，这里没有匿名分支。
     */
    private BoundHashOperations<String, Object, Object> cartOps() {
        return redisTemplate.boundHashOps(CART_PREFIX + currentUserId());
    }

    private Long currentUserId() {
        MemberResponseVo user = LoginUserInterceptor.loginUser.get();
        if (user == null || user.getId() == null) {
            // 正常不可达：LoginUserInterceptor 已经把所有 /cart/** 的匿名请求拦成 401 了。
            // 留着是因为这里依赖拦截器的注册，万一注册被改掉（比如有人把
            // addPathPatterns 改回 "/**" 之外的东西），要给出"没登录"而不是一个 NPE
            throw new BaseException(BaseCodeEnum.NOT_LOGIN_EXCEPTION);
        }
        return user.getId();
    }

    /** 车里所有购物项。空车返回空列表，不是 null */
    private List<CartItemVo> allItems() {
        List<Object> values = cartOps().values();
        if (values == null || values.isEmpty()) {
            return new ArrayList<>();
        }
        return values.stream()
                .map(value -> JSON.parseObject((String) value, CartItemVo.class))
                .collect(Collectors.toList());
    }

    private CartItemVo readItem(BoundHashOperations<String, Object, Object> cartOps, Long skuId) {
        String value = (String) cartOps.get(skuId.toString());
        return value == null ? null : JSON.parseObject(value, CartItemVo.class);
    }

    /**
     * 用商品服务的最新价格覆盖车里存的价格。
     *
     * <p>Redis 里存的是"加购那一刻的价格"，不刷新的话购物车页显示一个数、结算页显示另一个数。
     * 原实现只在下单取已勾选项时刷新，购物车页面看到的一直是加购时的旧价；这次两边都刷。</p>
     *
     * <p>串行查是照抄原来的写法：购物车件数不多，换成并行还要额外处理线程池排队和异常聚合。
     * 查价失败不吞异常 —— 宁可整个请求失败，也不要把过期价格当成真实价格算进总价。
     * 已知代价：某个 sku 被删掉之后这个购物车会一直打不开，要等"失效商品"那类功能补上才能自愈。</p>
     */
    private void refreshPrices(List<CartItemVo> items) {
        for (CartItemVo item : items) {
            item.setPrice(productFeignService.getPrice(item.getSkuId()));
        }
    }
}
