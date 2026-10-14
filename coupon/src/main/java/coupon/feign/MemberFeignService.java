package coupon.feign;

import common.to.MemberGrowthVo;
import common.to.MemberLevelVo;
import common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

/**
 * member 会员服务的 Feign 契约：按 id 批量取会员昵称，以及取会员的成长状态与等级门槛。
 *
 * <p>成长状态与等级门槛用于判断券的领取门槛：等级表在 member 库，本服务不读别人的表。
 *
 * <p>{@code @FeignClient} 取服务名，调用直连服务实例而不经网关 ——
 * 网关对非 {@code /front} 路径要求管理端凭证，服务间调用没有也不该有那个凭证。
 *
 * <p>返回值统一是 {@link R}：调用方必须先判 {@code code}，为 0 时才能读 {@code data}。
 */
@FeignClient("member")
public interface MemberFeignService {

    /**
     * 按会员 id 批量查询昵称。
     *
     * @param memberIds 会员 id 集合，不能为 {@code null}
     * @return 统一响应；{@code code} 为 0 时 {@code data} 是会员 id 到昵称的映射，
     *         查不到的 id 与昵称为空的会员都不在映射中（调用方取值得到 {@code null}）；
     *         {@code code} 非 0 时 {@code data} 为 {@code null}
     */
    @PostMapping(value = "/member/member/getMemberNames")
    R<Map<Long, String>> getMemberNames(@RequestBody List<Long> memberIds);

    /**
     * 查询会员的成长状态：当前成长值、所属等级与该等级的门槛。
     *
     * <p>不能改用登录凭证里的字段：凭证是登录那一刻的快照，攒够成长值升级之后就不准了。
     *
     * @param memberId 会员 id，不能为 {@code null}
     * @return 统一响应；{@code code} 为 0 时 {@code data} 是该会员的成长状态（成长值归一到 0），
     *         {@code code} 非 0 时 {@code data} 为 {@code null}
     */
    @GetMapping("/member/trade/growth/level")
    R<MemberGrowthVo> growthLevel(@RequestParam("memberId") Long memberId);

    /**
     * 查询全部会员等级定义，按成长值门槛升序。
     *
     * @return 统一响应；{@code code} 为 0 时 {@code data} 是等级定义列表（等级表为空时为空列表），
     *         {@code code} 非 0 时 {@code data} 为 {@code null}
     */
    @GetMapping("/member/trade/growth/levels")
    R<List<MemberLevelVo>> growthLevels();
}
