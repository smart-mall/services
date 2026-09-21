package cart.service;

import cart.vo.CartItemVo;
import cart.vo.CartVo;

import java.util.List;

/**
 * 购物车。所有方法都作用于"当前登录用户"的车 —— 没有匿名车，
 * 用户身份从 {@link cart.interceptor.LoginUserInterceptor} 的 ThreadLocal 里取。
 */
public interface CartService {

    /**
     * 加入购物车。车里已经有这个 sku 就累加数量，不会覆盖。
     *
     * @param num 增量，必须 ≥ 1（上限由 Controller 的校验注解负责）
     * @return 加购后的那一项
     */
    CartItemVo addToCart(Long skuId, Integer num);

    /**
     * 当前用户的整车（含未勾选项），价格已刷新到最新。
     */
    CartVo getCart();

    /**
     * 当前用户购物车中已勾选的购物项，价格已刷新到最新。订单确认页用。
     *
     * <p>车是空的时候返回空列表，不抛异常：空车是正常状态，
     * 要不要拦（比如"请先勾选商品再结算"）是调用方的事。</p>
     */
    List<CartItemVo> getCheckedCartItems();

    /**
     * 批量勾选 / 取消勾选。列表里不在车中的 skuId 会被跳过，不报错。
     */
    void checkItems(List<Long> skuIds, Boolean checked);

    /**
     * 修改某个购物项的数量。<b>绝对值</b>，不是增量。
     *
     * @throws common.exception.BaseException 车里没有这个 sku 时抛出 {@code CART_ITEM_NOT_FOUND}
     */
    void changeItemCount(Long skuId, Integer num);

    /**
     * 批量删除购物项。不存在的 skuId 直接忽略（Redis 的 HDEL 本来就是幂等的）。
     */
    void deleteCartItems(List<Long> skuIds);

}
