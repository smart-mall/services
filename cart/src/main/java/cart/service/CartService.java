package cart.service;

import cart.vo.CartItemVo;
import cart.vo.CartVo;
import common.vo.MemberResponseVo;

import java.util.List;

/**
 * 购物车服务：提供加购、查询、勾选、改数量与删除。
 *
 * <p>所有方法都作用于入参 {@link MemberResponseVo} 对应的会员，没有匿名购物车；
 * 会员身份由控制器从网关注入的 {@code X-Member-Claims} 头解析后传入。
 */
public interface CartService {

    /**
     * 把 SKU 加入购物车，车里已有该 SKU 时累加数量，不覆盖已有数量。
     *
     * <p>实现方需保证：{@code num} 按增量处理；SKU 不存在时不得写入购物车，应直接抛异常。
     *
     * @param user  会员身份，不能为 {@code null}，其 {@code id} 不能为 {@code null}
     * @param skuId 商品 SKU 标识，不能为 {@code null}
     * @param num   加购数量增量，取值区间由 Controller 的校验注解保证
     * @return 加购后的购物项，数量为累加后的值
     */
    CartItemVo addToCart(MemberResponseVo user, Long skuId, Integer num);

    /**
     * 返回会员的整车，含未勾选项，价格已刷新到最新。
     *
     * @param user 会员身份，不能为 {@code null}
     * @return 整车信息；空车时 {@code items} 为空列表
     */
    CartVo getCart(MemberResponseVo user);

    /**
     * 返回会员购物车中已勾选的购物项，价格已刷新到最新。
     *
     * <p>订单确认页使用，order 服务通过 Feign 调用。
     *
     * @param user 会员身份，不能为 {@code null}
     * @return 已勾选的购物项；没有勾选项时为空列表
     */
    List<CartItemVo> getCheckedCartItems(MemberResponseVo user);

    /**
     * 批量勾选或取消勾选购物项。
     *
     * <p>实现方需保证：{@code skuIds} 中不在车里的 SKU 直接跳过，不抛异常。
     *
     * @param user    会员身份，不能为 {@code null}
     * @param skuIds  待修改的 SKU 标识；为 {@code null} 或空列表时不做任何事
     * @param checked {@code true} 表示勾选，{@code false} 表示取消勾选
     */
    void checkItems(MemberResponseVo user, List<Long> skuIds, Boolean checked);

    /**
     * 把某个购物项的数量设为给定值，为绝对值而非增量。
     *
     * @param user  会员身份，不能为 {@code null}
     * @param skuId 商品 SKU 标识，不能为 {@code null}
     * @param num   目标数量，取值区间由 Controller 的校验注解保证
     * @throws common.exception.BaseException 车里没有该 SKU 时抛出 {@code CART_ITEM_NOT_FOUND}
     */
    void changeItemCount(MemberResponseVo user, Long skuId, Integer num);

    /**
     * 批量删除购物项。
     *
     * <p>实现方需保证：不存在的 SKU 直接忽略，重复删除同样成功。
     *
     * @param user   会员身份，不能为 {@code null}
     * @param skuIds 待删除的 SKU 标识；为 {@code null} 或空列表时不做任何事
     */
    void deleteCartItems(MemberResponseVo user, List<Long> skuIds);

}
