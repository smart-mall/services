package common.to;

import lombok.Data;

/**
 * 评价来源快照：product 提交评价前向 order 核对"这个订单项能不能评价"，order 核对通过后下发本对象。
 *
 * <p>放在 {@code common/to} 是它是 product 与 order 之间的契约：两个服务各存一份副本时，一边加字段、
 * 另一边没加不会编译报错，Jackson 只会把缺的字段读成 {@code null}，联调也看不出来。
 *
 * <p>只带写评价需要的字段。归属（这项是不是该会员的）与状态（订单是否已完成）两个判据由 order
 * 先判完，不通过直接抛业务异常，因此本对象里没有可被调用方二次解读的 {@code status} 之类字段。
 */
@Data
public class CommentTargetVo {

    /** 订单号，写进评价行作为来源。 */
    private String orderSn;
    /** 订单项 ID，写进评价行的 {@code order_item_id}，也是"一张订单里一件商品只能评价一次"的唯一键。 */
    private Long orderItemId;
    /** 被评价的 SKU ID。 */
    private Long skuId;
    /** 被评价的 SPU ID。 */
    private Long spuId;
    /** 商品名快照，取自下单时的订单项，不重新查商品。 */
    private String spuName;
    /** 购买时的销售属性组合，取自订单项的 {@code sku_attrs_vals}。 */
    private String spuAttributes;
    /** 下单会员名，取自订单的 {@code member_username}；秒杀单该列为空时为 {@code null}。 */
    private String memberUsername;

}
