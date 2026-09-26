package cart.controller;

import cart.service.CartService;
import cart.vo.AddCartItemVo;
import cart.vo.ChangeItemCountVo;
import cart.vo.CheckItemVo;
import cart.vo.CheckItemsVo;
import common.utils.LoginUserUtils;
import common.utils.R;
import common.vo.MemberResponseVo;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import cart.vo.CartItemVo;
import cart.vo.CartVo;
/**
 * 购物车前台接口，路径前缀为 {@code cart/front/jwt}，所有操作都要求登录。
 *
 * <p>写操作统一返回整车 {@link CartVo}，前端不必自己重算总价与件数。
 */
@Validated
@RestController
@RequestMapping("cart/front/jwt")
public class CartController {

    private final CartService cartService;

    /**
     * 注入购物车服务。
     *
     * @param cartService 购物车服务，不能为 {@code null}
     */
    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    /**
     * 返回当前会员的整车，含未勾选项。
     *
     * @param request 当前请求，用于取网关注入的会员身份
     * @return 整车信息；空车时 {@code items} 为空列表
     */
    @GetMapping("/list")
    public R<CartVo> list(HttpServletRequest request) {
        return R.ok(cartService.getCart(LoginUserUtils.requireCurrentUser(request)));
    }

    /**
     * 返回已勾选的购物项，价格刷新到最新。
     *
     * <p>订单确认页使用，order 服务通过 Feign 调用本接口。
     *
     * @param request 当前请求，用于取网关注入的会员身份
     * @return 已勾选的购物项；没有勾选项时为空列表
     */
    @GetMapping("/checked")
    public R<List<CartItemVo>> checked(HttpServletRequest request) {
        return R.ok(cartService.getCheckedCartItems(LoginUserUtils.requireCurrentUser(request)));
    }

    /**
     * 把指定 SKU 加入购物车，车里已有该 SKU 时累加数量。
     *
     * @param request 当前请求，用于取网关注入的会员身份
     * @param vo      加购请求体，{@code num} 为数量增量
     * @return 加购后的整车
     */
    @PostMapping("/items")
    public R<CartVo> addItem(HttpServletRequest request, @Valid @RequestBody AddCartItemVo vo) {
        MemberResponseVo user = LoginUserUtils.requireCurrentUser(request);
        cartService.addToCart(user, vo.getSkuId(), vo.getNum());
        return R.ok(cartService.getCart(user));
    }

    /**
     * 把指定 SKU 的数量设为给定值。
     *
     * <p>{@code num} 是绝对值，不是增量。
     *
     * @param request 当前请求，用于取网关注入的会员身份
     * @param skuId   商品 SKU 标识
     * @param vo      数量请求体
     * @return 修改后的整车
     */
    @PutMapping("/items/{skuId}/count")
    public R<CartVo> changeCount(HttpServletRequest request, @PathVariable("skuId") Long skuId,
                         @Valid @RequestBody ChangeItemCountVo vo) {
        MemberResponseVo user = LoginUserUtils.requireCurrentUser(request);
        cartService.changeItemCount(user, skuId, vo.getNum());
        return R.ok(cartService.getCart(user));
    }

    /**
     * 勾选或取消勾选单个购物项。
     *
     * @param request 当前请求，用于取网关注入的会员身份
     * @param skuId   商品 SKU 标识
     * @param vo      勾选请求体
     * @return 勾选后的整车
     */
    @PutMapping("/items/{skuId}/check")
    public R<CartVo> checkItem(HttpServletRequest request, @PathVariable("skuId") Long skuId,
                       @Valid @RequestBody CheckItemVo vo) {
        MemberResponseVo user = LoginUserUtils.requireCurrentUser(request);
        cartService.checkItems(user, List.of(skuId), vo.getChecked());
        return R.ok(cartService.getCart(user));
    }

    /**
     * 批量勾选或取消勾选，全选与反选也走这里。
     *
     * <p>本方法路径比单项勾选少一段 {@code /{skuId}}，两者不会产生映射歧义。
     *
     * @param request 当前请求，用于取网关注入的会员身份
     * @param vo      勾选请求体，{@code skuIds} 必须显式给出
     * @return 勾选后的整车
     */
    @PutMapping("/items/check")
    public R<CartVo> checkItems(HttpServletRequest request, @Valid @RequestBody CheckItemsVo vo) {
        MemberResponseVo user = LoginUserUtils.requireCurrentUser(request);
        cartService.checkItems(user, vo.getSkuIds(), vo.getChecked());
        return R.ok(cartService.getCart(user));
    }

    /**
     * 删除单个购物项。
     *
     * @param request 当前请求，用于取网关注入的会员身份
     * @param skuId   商品 SKU 标识
     * @return 删除后的整车
     */
    @DeleteMapping("/items/{skuId}")
    public R<CartVo> deleteItem(HttpServletRequest request, @PathVariable("skuId") Long skuId) {
        MemberResponseVo user = LoginUserUtils.requireCurrentUser(request);
        cartService.deleteCartItems(user, List.of(skuId));
        return R.ok(cartService.getCart(user));
    }

    /**
     * 批量删除购物项。
     *
     * <p>{@code skuIds} 走查询参数而不是请求体：部分代理会丢弃 DELETE 的 body。
     *
     * @param request 当前请求，用于取网关注入的会员身份
     * @param skuIds  待删除的 SKU 标识，不能为空
     * @return 删除后的整车
     */
    @DeleteMapping("/items")
    public R<CartVo> deleteItems(HttpServletRequest request,
                         @RequestParam("skuIds") @NotEmpty(message = "不能为空") List<Long> skuIds) {
        MemberResponseVo user = LoginUserUtils.requireCurrentUser(request);
        cartService.deleteCartItems(user, skuIds);
        return R.ok(cartService.getCart(user));
    }
}
