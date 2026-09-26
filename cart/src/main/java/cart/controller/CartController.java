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
/** 购物车接口（前台 Vue 用），全部要求登录。写操作返回整车 CartVo，前端不用自己重算总价和件数。 */
@Validated
@RestController
@RequestMapping("cart/front/jwt")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    /** 整车（含未勾选项） */
    @GetMapping("/list")
    public R<CartVo> list(HttpServletRequest request) {
        return R.ok(cartService.getCart(LoginUserUtils.requireCurrentUser(request)));
    }

    /** 已勾选的购物项，价格刷新到最新。订单确认页用（order 通过 Feign 调） */
    @GetMapping("/checked")
    public R<List<CartItemVo>> checked(HttpServletRequest request) {
        return R.ok(cartService.getCheckedCartItems(LoginUserUtils.requireCurrentUser(request)));
    }

    /** 加购。车里已有这个 sku 就累加数量 */
    @PostMapping("/items")
    public R<CartVo> addItem(HttpServletRequest request, @Valid @RequestBody AddCartItemVo vo) {
        MemberResponseVo user = LoginUserUtils.requireCurrentUser(request);
        cartService.addToCart(user, vo.getSkuId(), vo.getNum());
        return R.ok(cartService.getCart(user));
    }

    /** 改数量。绝对值，不是增量 */
    @PutMapping("/items/{skuId}/count")
    public R<CartVo> changeCount(HttpServletRequest request, @PathVariable("skuId") Long skuId,
                         @Valid @RequestBody ChangeItemCountVo vo) {
        MemberResponseVo user = LoginUserUtils.requireCurrentUser(request);
        cartService.changeItemCount(user, skuId, vo.getNum());
        return R.ok(cartService.getCart(user));
    }

    /** 勾选 / 取消勾选单项 */
    @PutMapping("/items/{skuId}/check")
    public R<CartVo> checkItem(HttpServletRequest request, @PathVariable("skuId") Long skuId,
                       @Valid @RequestBody CheckItemVo vo) {
        MemberResponseVo user = LoginUserUtils.requireCurrentUser(request);
        cartService.checkItems(user, List.of(skuId), vo.getChecked());
        return R.ok(cartService.getCart(user));
    }

    /** 批量勾选 / 全选反选。和单项勾选的段数不同，Spring 不会歧义 */
    @PutMapping("/items/check")
    public R<CartVo> checkItems(HttpServletRequest request, @Valid @RequestBody CheckItemsVo vo) {
        MemberResponseVo user = LoginUserUtils.requireCurrentUser(request);
        cartService.checkItems(user, vo.getSkuIds(), vo.getChecked());
        return R.ok(cartService.getCart(user));
    }

    /** 删除单项 */
    @DeleteMapping("/items/{skuId}")
    public R<CartVo> deleteItem(HttpServletRequest request, @PathVariable("skuId") Long skuId) {
        MemberResponseVo user = LoginUserUtils.requireCurrentUser(request);
        cartService.deleteCartItems(user, List.of(skuId));
        return R.ok(cartService.getCart(user));
    }

    /** 批量删除。skuIds 走查询参数：有些代理会把 DELETE 的 body 丢掉 */
    @DeleteMapping("/items")
    public R<CartVo> deleteItems(HttpServletRequest request,
                         @RequestParam("skuIds") @NotEmpty(message = "不能为空") List<Long> skuIds) {
        MemberResponseVo user = LoginUserUtils.requireCurrentUser(request);
        cartService.deleteCartItems(user, skuIds);
        return R.ok(cartService.getCart(user));
    }
}
