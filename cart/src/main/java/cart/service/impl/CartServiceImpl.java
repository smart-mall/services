package cart.service.impl;

import cart.feign.ProductFeignService;
import cart.service.CartService;
import cart.vo.CartItemVo;
import cart.vo.CartVo;
import cart.vo.SkuInfoVo;
import com.alibaba.fastjson.JSON;
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
    public CartItemVo addToCart(MemberResponseVo user, Long skuId, Integer num) {
        BoundHashOperations<String, Object, Object> cartOps = cartOps(user);

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
            R<SkuInfoVo> productSkuInfo = productFeignService.getInfo(skuId);
            SkuInfoVo skuInfo = productSkuInfo.getData();
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

        // 在这里把 ExecutionException 收口成 BaseException，走统一的 {code, msg}：
        // 它和 InterruptedException 都没有 @ExceptionHandler，直接抛会落到 Spring 默认错误页
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
    public CartVo getCart(MemberResponseVo user) {
        List<CartItemVo> items = allItems(user);
        refreshPrices(items);

        CartVo cartVo = new CartVo();
        cartVo.setItems(items);
        return cartVo;
    }

    @Override
    public List<CartItemVo> getCheckedCartItems(MemberResponseVo user) {
        List<CartItemVo> checked = allItems(user).stream()
                .filter(item -> Boolean.TRUE.equals(item.getCheck()))
                .collect(Collectors.toList());
        refreshPrices(checked);
        return checked;
    }

    @Override
    public void checkItems(MemberResponseVo user, List<Long> skuIds, Boolean checked) {
        if (skuIds == null || skuIds.isEmpty()) {
            return;
        }
        BoundHashOperations<String, Object, Object> cartOps = cartOps(user);
        for (Long skuId : skuIds) {
            CartItemVo item = readItem(cartOps, skuId);
            if (item == null) {
                // 不报错：前端的列表可能已经过期（另一个标签页删掉了商品），
                // 为一个失效的 skuId 让整次"全选"失败，用户会以为勾选功能坏了
                log.debug("勾选时跳过购物车中不存在的商品：skuId={}", skuId);
                continue;
            }
            item.setCheck(checked);
            cartOps.put(skuId.toString(), JSON.toJSONString(item));
        }
    }

    @Override
    public void changeItemCount(MemberResponseVo user, Long skuId, Integer num) {
        BoundHashOperations<String, Object, Object> cartOps = cartOps(user);
        CartItemVo item = readItem(cartOps, skuId);
        if (item == null) {
            // 改数量是"把这个 sku 的数量设成 N"，车里没有它就说明前端状态已经错了，得让它知道
            throw new BaseException(BaseCodeEnum.CART_ITEM_NOT_FOUND);
        }
        item.setCount(num);
        cartOps.put(skuId.toString(), JSON.toJSONString(item));
    }

    @Override
    public void deleteCartItems(MemberResponseVo user, List<Long> skuIds) {
        if (skuIds == null || skuIds.isEmpty()) {
            return;
        }
        Object[] fields = skuIds.stream().map(String::valueOf).toArray();
        cartOps(user).delete(fields);
    }

    /** 会员的购物车 Redis Hash：key = {@code gulimall:cart:<userId>} */
    private BoundHashOperations<String, Object, Object> cartOps(MemberResponseVo user) {
        return redisTemplate.boundHashOps(CART_PREFIX + user.getId());
    }

    /** 车里所有购物项。空车返回空列表，不是 null */
    private List<CartItemVo> allItems(MemberResponseVo user) {
        List<Object> values = cartOps(user).values();
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

    /** 用商品服务的最新价覆盖车里存的价：Redis 里是加购那一刻的价，不刷新会和结算页对不上 */
    private void refreshPrices(List<CartItemVo> items) {
        for (CartItemVo item : items) {
            item.setPrice(productFeignService.getPrice(item.getSkuId()));
        }
    }
}
