package common.constant;

/**
 * 购物车常量。
 *
 * <p>购物车一律要求登录：{@link #CART_PREFIX} 拼上 userId 作为 Redis Hash 的 key，
 * 没有匿名购物车这条路径。
 *
 * <p>{@link #CART_PREFIX} 的值不能改：order 下单成功后直接按这个 key 删除
 * （见 {@code OrderServiceImpl#submitOrder}），改了会让已下单用户的购物车清不掉。
 */
public class CartConstant {

    /** 购物车 Redis Hash 的 key 前缀，拼上 userId 使用 */
    public final static String CART_PREFIX = "gulimall:cart:";

    /** 单个购物项的数量下限 */
    public final static int MIN_ITEM_COUNT = 1;

    /** 单个购物项的数量上限 */
    public final static int MAX_ITEM_COUNT = 99;

}
