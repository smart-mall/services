package cart.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;


/**
 * 购物车。
 *
 * <p>{@code countNum} / {@code countType} / {@code totalAmount} 都不是字段而是计算属性，
 * 由下面手写的方法提供（Lombok 检测到同名方法后不会生成 getter），每次取都重新算。
 * 好处是调用方不用记得"改完 items 要同步汇总"；代价是 Jackson 序列化时会各算一遍，
 * 购物车件数很少，这点开销可以忽略。</p>
 */
@Data
public class CartVo {

    /**
     * 购物车子项信息。默认空列表而不是 null —— 前端拿到的永远是数组，
     * 不用为"车是空的"和"字段没返回"写两个分支。
     */
    private List<CartItemVo> items = new ArrayList<>();

    /**
     * 减免金额，当前没有写入方，实际恒为 {@code 0}；
     * {@link #getTotalAmount()} 会把它从总价里减掉。
     */
    private BigDecimal reduce = BigDecimal.ZERO;

    /**
     * 返回商品总件数，为各购物项数量之和，不区分是否勾选。
     *
     * @return 总件数；购物车为空或某项数量缺失时按 0 计
     */
    public Integer getCountNum() {
        if (items == null || items.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (CartItemVo item : items) {
            if (item.getCount() != null) {
                count += item.getCount();
            }
        }
        return count;
    }

    /**
     * 返回商品种类数，即购物车中不同 SKU 的个数。
     *
     * @return 种类数；{@code items} 为 {@code null} 时返回 0
     */
    public Integer getCountType() {
        return items == null ? 0 : items.size();
    }

    /**
     * 返回应付总价，等于勾选项小计之和减去减免金额。
     *
     * <p>只算勾选项：未勾选的商品不计入结算金额。判断用 {@code Boolean.TRUE.equals}
     * 而不是直接拆箱 {@code getCheck()}，因为 {@code check} 是包装类型，
     * Redis 中已有的 JSON 可能缺该字段，拆箱会 NPE。</p>
     *
     * @return 应付总价，不会为 {@code null}
     */
    public BigDecimal getTotalAmount() {
        BigDecimal amount = BigDecimal.ZERO;
        if (items != null) {
            for (CartItemVo item : items) {
                if (Boolean.TRUE.equals(item.getCheck())) {
                    amount = amount.add(item.getTotalPrice());
                }
            }
        }
        return amount.subtract(reduce == null ? BigDecimal.ZERO : reduce);
    }

}
