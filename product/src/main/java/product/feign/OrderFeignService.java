package product.feign;

import common.to.CommentTargetVo;
import common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * order 服务的 Feign 客户端：会员提交评价前核对来源，评价落库后登记订单的评价时间。
 *
 * <p>调用直连订单服务实例、不经网关，没有会员凭证可带，所以会员身份由本客户端显式传参。
 *
 * <p>返回值统一是 {@link R}，业务是否成功看 {@code code}，取数必须读 {@code data}。
 */
@FeignClient("order")
public interface OrderFeignService {

    /**
     * 核对某个订单项能不能评价，并取回写评价所需的商品快照。
     *
     * <p>只读接口，不修改任何数据。
     *
     * @param orderItemId 订单项主键，不能为 {@code null}
     * @param memberId 当前登录会员 ID，不能为 {@code null}
     * @return 统一响应体，{@code data} 为评价来源快照；订单项不存在或不属于该会员、订单未完成时
     *         {@code code} 非 0 且 {@code data} 为 {@code null}；调用失败时整个对象为 {@code null}
     */
    @GetMapping("/order/order/comment-target/{orderItemId}")
    R<CommentTargetVo> getCommentTarget(@PathVariable("orderItemId") Long orderItemId,
                                        @RequestParam("memberId") Long memberId);

    /**
     * 登记订单的评价时间。
     *
     * <p>幂等：重复调用只更新 0 行且返回成功。订单侧只登记时间，不写评价行。
     *
     * @param orderSn 订单号，不能为 {@code null}
     * @param memberId 当前登录会员 ID，不能为 {@code null}
     * @return 统一响应体，{@code data} 为空
     */
    @PostMapping("/order/order/comment-marked")
    R<Void> markCommented(@RequestParam("orderSn") String orderSn,
                          @RequestParam("memberId") Long memberId);

    /**
     * 撤回订单上的评价时间，供会员删除自己的评价后调用。
     *
     * <p>幂等：订单上本来就没有评价时间时只更新 0 行且返回成功。按订单项定位订单，
     * 因为评价行里只有 {@code order_item_id}、没有订单号。</p>
     *
     * @param orderItemId 来源订单项主键，不能为 {@code null}
     * @param memberId 当前登录会员 ID，不能为 {@code null}
     * @return 统一响应体，{@code data} 为空
     */
    @PostMapping("/order/order/comment-unmark/{orderItemId}")
    R<Void> unmarkCommented(@PathVariable("orderItemId") Long orderItemId,
                            @RequestParam("memberId") Long memberId);

}
