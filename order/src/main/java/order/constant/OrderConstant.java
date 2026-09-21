package order.constant;

/**
 * @Description:
 * @Created: with IntelliJ IDEA.
 * @author: 夏沫止水
 * @createTime: 2020-07-04 11:44
 **/
public class OrderConstant {

    public static final String USER_ORDER_TOKEN_PREFIX = "order:token";

    /**
     * 防重令牌的有效期（分钟）。
     *
     * <p>必须不小于"关单超时"：令牌是确认页发的，用户填完地址、点提交时它还得有效。
     * 原来写死 30 分钟，而关单 TTL 是 1 分钟（见 RabbitMQConfig），两者是脱节的。</p>
     */
    public static final int USER_ORDER_TOKEN_TIMEOUT_MINUTES = 30;

    /** 我的订单默认每页条数 */
    public static final int DEFAULT_PAGE_SIZE = 10;

    /** 我的订单每页条数上限，防止 pageSize=100000 把整表捞出来 */
    public static final int MAX_PAGE_SIZE = 100;

}
