package common.constant;

/**
 * 购物车常量。
 *
 * <p>"未登录也能加购"的临时购物车已经砍掉。原来靠一个 {@code user-key} cookie 认访客，
 * 而 cookie 想跨子域共享就得写死 {@code Domain=gulimall.com}：前台换成 Vue 之后页面在
 * localhost、接口在 localhost:53000，Domain 匹配不上、又是跨源，浏览器直接丢弃这个 cookie，
 * 结果是每次请求都发一个新的 user-key、匿名购物车永远是空的。访客身份本身也带不过设备，
 * 收益配不上这一串坑，所以购物车一律要求登录。</p>
 *
 * <p>因此这里删掉了 {@code TEMP_USER_COOKIE_NAME} 和 {@code TEMP_USER_COOKIE_TIMEOUT}。
 * {@code CART_PREFIX} 保留原值：order 下单成功后是直接删这个 key 的
 * （见 OrderServiceImpl#submitOrder），改了会让已下单用户的购物车清不掉。</p>
 */
public class CartConstant {

    public final static String CART_PREFIX = "gulimall:cart:";

    /** 单个购物项的数量下限 */
    public final static int MIN_ITEM_COUNT = 1;

    /** 单个购物项的数量上限 */
    public final static int MAX_ITEM_COUNT = 99;

}
