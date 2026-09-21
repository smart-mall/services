package cart.controller;

import cart.service.CartService;
import cart.vo.AddCartItemVo;
import cart.vo.ChangeItemCountVo;
import cart.vo.CheckItemVo;
import cart.vo.CheckItemsVo;
import common.utils.R;
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

/**
 * 购物车接口（前台 Vue 用）。
 *
 * <p>原来是 {@code @Controller} + Thymeleaf：{@code /cart.html} 返回视图 cartList，
 * 加购 / 改数量 / 删除全是 GET 加一个 302 跳到 {@code http://cart.gulimall.com/...}。
 * 前台换成 Vue、请求变成跨源 XHR 之后这两套都不能用：视图名前端解析不了，
 * 302 到 cart.gulimall.com 要么打不开，要么把响应变成一段 HTML 让 JSON 解析崩掉。</p>
 *
 * <p>现在全是 JSON 接口，写操作按语义用 POST/PUT/DELETE，不再用 GET ——
 * GET 做写操作会被浏览器预取、被中间层缓存，语义上也不该有副作用。</p>
 *
 * <p>每个写操作都返回<b>整车</b> {@code CartVo} 而不是只返回被改的那一项：
 * 前端只维护一份状态、不用自己重算总价和件数，也就不会出现"数量改了但总价没跟着变"。</p>
 *
 * <p>路径带 cart 前缀才能被网关的 cart-api-route（Path=/api/cart/** 且会剥掉 /api）命中：
 * {@code GET /api/cart/list} → {@code /cart/list}。</p>
 */
@Validated
@RestController
@RequestMapping("cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    /** 整车（含未勾选项） */
    @GetMapping("/list")
    public R list() {
        return R.ok().setData(cartService.getCart());
    }

    /**
     * 已勾选的购物项，价格刷新到最新。订单确认页用。
     *
     * <p>以前叫 {@code GET /currentUserCartItems}，返回裸 List，是全项目唯一一个
     * 不套 {@code R} 的接口。统一之后旧路径删掉了，order 的 CartFeignService 改调这里。</p>
     */
    @GetMapping("/checked")
    public R checked() {
        return R.ok().setData(cartService.getCheckedCartItems());
    }

    /** 加购。车里已有这个 sku 就累加数量 */
    @PostMapping("/items")
    public R addItem(@Valid @RequestBody AddCartItemVo vo) {
        cartService.addToCart(vo.getSkuId(), vo.getNum());
        return R.ok().setData(cartService.getCart());
    }

    /** 改数量。绝对值，不是增量 */
    @PutMapping("/items/{skuId}/count")
    public R changeCount(@PathVariable("skuId") Long skuId,
                         @Valid @RequestBody ChangeItemCountVo vo) {
        cartService.changeItemCount(skuId, vo.getNum());
        return R.ok().setData(cartService.getCart());
    }

    /** 勾选 / 取消勾选单项 */
    @PutMapping("/items/{skuId}/check")
    public R checkItem(@PathVariable("skuId") Long skuId,
                       @Valid @RequestBody CheckItemVo vo) {
        cartService.checkItems(List.of(skuId), vo.getChecked());
        return R.ok().setData(cartService.getCart());
    }

    /**
     * 批量勾选 / 全选反选。
     *
     * <p>路径是 {@code /cart/items/check}，和上面的 {@code /cart/items/{skuId}/check}
     * 段数不同（3 段 vs 4 段），Spring 不会歧义。</p>
     */
    @PutMapping("/items/check")
    public R checkItems(@Valid @RequestBody CheckItemsVo vo) {
        cartService.checkItems(vo.getSkuIds(), vo.getChecked());
        return R.ok().setData(cartService.getCart());
    }

    /** 删除单项 */
    @DeleteMapping("/items/{skuId}")
    public R deleteItem(@PathVariable("skuId") Long skuId) {
        cartService.deleteCartItems(List.of(skuId));
        return R.ok().setData(cartService.getCart());
    }

    /**
     * 批量删除。
     *
     * <p>skuIds 走查询参数（{@code ?skuIds=1&skuIds=2}）而不是 DELETE 的 body：
     * 带 body 的 DELETE 虽然合法，但 axios 要额外写 {@code config.data}，
     * 而且有些代理会直接把 DELETE 的 body 丢掉，得靠"删不成功"才发现。</p>
     */
    @DeleteMapping("/items")
    public R deleteItems(@RequestParam("skuIds") @NotEmpty(message = "不能为空") List<Long> skuIds) {
        cartService.deleteCartItems(skuIds);
        return R.ok().setData(cartService.getCart());
    }

}
