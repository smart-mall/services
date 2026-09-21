package cart.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;


/**
 * 购物车中一项。
 *
 * <p>这个对象会被序列化成 JSON 存进 Redis（key = {@code gulimall:cart:<userId>}，field = skuId），
 * 所以字段增删要考虑到旧数据：新加字段在老 JSON 里不存在，反序列化出来是 null，
 * 下面 {@link #getTotalPrice()} 的判空就是为这个准备的。</p>
 */
@Data
public class CartItemVo {

    private Long skuId;

    /** 是否勾选参与结算。默认勾上 —— 加购的意图就是要买 */
    private Boolean check = true;

    private String title;

    private String image;

    /**
     * 商品套餐属性，形如 ["颜色: 黑色", "内存: 8GB"]
     */
    private List<String> skuAttrValues;

    /**
     * 价格。注意这是"加购那一刻"的价格，读购物车时会被商品服务的最新价格覆盖
     * （见 CartServiceImpl#refreshPrices），但 Redis 里存的始终是加购时的值。
     */
    private BigDecimal price;

    private Integer count;

    /**
     * 小计。这个字段<b>永远不会被读</b> —— {@link #getTotalPrice()} 是手写的，
     * Lombok 那边不会再生成 getter，所以序列化时算的是 price × count，字段里的值只是摆设。
     *
     * <p>那为什么留着：它是<b>反序列化的兼容位</b>。Redis 里存的老购物车 JSON
     * 是带 {@code "totalPrice"} 的，而 fastjson 对一个"没有对应字段、也没有 setter"
     * 的属性是忽略还是报错，取决于版本和 Feature 开关。留着这个字段就不用赌这件事，
     * 反正它占不了几个字节。</p>
     */
    private BigDecimal totalPrice;

    /**
     * 小计 = 单价 × 数量。
     *
     * <p>是计算属性不是存储字段：无论谁怎么改 price / count，读出来都是自洽的，
     * 不会出现"字段和价格算出来的对不上"。</p>
     */
    public BigDecimal getTotalPrice() {
        // 判空：加购时商品服务可能没返回价格；Redis 里的老 JSON 也可能缺字段
        if (price == null || count == null) {
            return BigDecimal.ZERO;
        }
        return price.multiply(BigDecimal.valueOf(count));
    }

}
