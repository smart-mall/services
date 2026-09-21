package cart.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;


/**
 * 购物车。
 *
 * <p>{@code countNum} / {@code countType} / {@code totalAmount} 都不是字段而是计算属性
 * （Lombok 生成的 getter 被下面手写的方法覆盖了），每次取都重新算。
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
     * 减免价格。优惠券还没做，恒为 0，但 {@link #getTotalAmount()} 会减掉它，
     * 留着是为了以后接优惠券时不用改总价的算法。
     */
    private BigDecimal reduce = BigDecimal.ZERO;

    /** 商品总件数（各购物项数量之和，不区分是否勾选） */
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

    /** 商品种类数（有几个不同的 sku） */
    public Integer getCountType() {
        return items == null ? 0 : items.size();
    }

    /**
     * 应付总价 = 勾选项小计之和 - 减免。
     *
     * <p>只算勾选的：购物车里没勾的商品不该进结算金额。判断用
     * {@code Boolean.TRUE.equals} 而不是直接 {@code getCheck()}，
     * 因为 check 是包装类型，历史数据里可能是 null，拆箱会 NPE。</p>
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
