package cart.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;


/**
 * 购物车中的一项。
 *
 * <p>本对象被序列化成 JSON 存进 Redis（key = {@code gulimall:cart:<userId>}，field = skuId），
 * 所以字段增删要容忍 Redis 中已有的 JSON：缺失的字段反序列化出来是 {@code null}，
 * {@link #getTotalPrice()} 的判空就是为这个准备的。</p>
 */
@Data
public class CartItemVo {

    /** 商品 SKU 标识 */
    private Long skuId;

    /** 是否勾选参与结算。默认勾选 —— 加购的意图就是要买 */
    private Boolean check = true;

    /** 商品标题 */
    private String title;

    /** 商品图片地址 */
    private String image;

    /** 商品套餐属性，每项形如 {@code 颜色：黑色}（商品服务用全角冒号拼接） */
    private List<String> skuAttrValues;

    /** 加购时的价格，读取购物车时被商品服务的最新价覆盖（见 {@code CartServiceImpl#refreshPrices}） */
    private BigDecimal price;

    /** 购买数量 */
    private Integer count;

    /**
     * 小计，仅为反序列化兼容保留，字段中的值不会被读取。
     *
     * <p>{@link #getTotalPrice()} 是手写方法，Lombok 不会生成同名 getter，取值一律按
     * {@code price × count} 计算；保留该字段是为了让 Redis 中可能已存在的 {@code totalPrice}
     * 有对应属性，不必依赖 fastjson 忽略未知字段的开关。</p>
     */
    private BigDecimal totalPrice;

    /**
     * 返回本项小计，等于单价乘以数量。
     *
     * <p>是计算属性而不是存储字段：无论 price / count 被如何修改，读出来都自洽，
     * 不会出现字段值与价格算出来的对不上。</p>
     *
     * @return 小计；{@code price} 或 {@code count} 缺失时返回 {@code BigDecimal.ZERO}
     */
    public BigDecimal getTotalPrice() {
        // 判空：商品服务可能没返回价格，Redis 中已有的 JSON 也可能缺字段
        if (price == null || count == null) {
            return BigDecimal.ZERO;
        }
        return price.multiply(BigDecimal.valueOf(count));
    }

}
