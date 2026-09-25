package cart.service;

import cart.vo.CartItemVo;
import cart.vo.CartVo;
import common.vo.MemberResponseVo;

import java.util.List;

/** 购物车。所有方法都作用于传进来的会员的车 —— 身份由控制器从 {@code X-Member-Claims} 取，没有匿名车。 */
public interface CartService {

    /**
     * 加入购物车。车里已经有这个 sku 就累加数量，不会覆盖。
     *
     * @param num 增量，必须 ≥ 1（上限由 Controller 的校验注解负责）
     * @return 加购后的那一项
     */
    CartItemVo addToCart(MemberResponseVo user, Long skuId, Integer num);

    /** 会员的整车（含未勾选项），价格已刷新到最新 */
    CartVo getCart(MemberResponseVo user);

    /** 会员购物车中已勾选的购物项，价格已刷新到最新。订单确认页用 */
    List<CartItemVo> getCheckedCartItems(MemberResponseVo user);

    /** 批量勾选 / 取消勾选。列表里不在车中的 skuId 会被跳过，不报错 */
    void checkItems(MemberResponseVo user, List<Long> skuIds, Boolean checked);

    /**
     * 修改某个购物项的数量。<b>绝对值</b>，不是增量。
     *
     * @throws common.exception.BaseException 车里没有这个 sku 时抛出 {@code CART_ITEM_NOT_FOUND}
     */
    void changeItemCount(MemberResponseVo user, Long skuId, Integer num);

    /** 批量删除购物项。不存在的 skuId 直接忽略（Redis 的 HDEL 本来就是幂等的） */
    void deleteCartItems(MemberResponseVo user, List<Long> skuIds);

}
