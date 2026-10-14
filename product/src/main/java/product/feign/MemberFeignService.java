package product.feign;

import common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * member 会员服务的 Feign 契约，只用于评价成功后赠送成长值。
 *
 * <p>{@code @FeignClient} 取服务名，调用直连服务实例而不经网关 ——
 * 网关对非 {@code /front} 路径要求管理端凭证，服务间调用没有也不该有那个凭证。
 *
 * <p>返回值统一是 {@link R}：调用方必须先判 {@code code}，为 0 时才表示赠送已受理。
 */
@FeignClient("member")
public interface MemberFeignService {

    /**
     * 评价成功后赠送成长值。
     *
     * <p>不传数量：送多少取自会员当前等级配置的每次评价成长值，规则留在 member 侧。
     * 会员没有等级、或该等级把这一项配成 0 时 member 侧按无操作处理，应答仍是成功。
     *
     * <p>幂等：同一订单重复调用只赠一次，重试安全。
     *
     * @param memberId 会员 id，不能为 {@code null}
     * @param orderSn 订单号，不能为 {@code null}；幂等判据
     * @return 统一响应；{@code code} 为 0 表示赠送已受理，非 0 时 {@code data} 为 {@code null}
     */
    @PostMapping("/member/trade/growth/comment-grant")
    R<Void> grantGrowthByComment(@RequestParam("memberId") Long memberId,
                                 @RequestParam("orderSn") String orderSn);
}
