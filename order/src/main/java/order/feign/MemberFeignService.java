package order.feign;

import common.utils.R;
import order.vo.MemberAddressVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;


/**
 * 会员服务的远程调用接口，覆盖收货地址与积分两件事。
 *
 * <p>两类接口的凭证口径不同，不要混着用：地址接口按 {@code memberId} 查或写，member 侧不做归属校验，
 * 调用方必须自己确认这个 id 是当前会员；积分接口在 {@code /member/trade/integration} 下，
 * 不经网关、没有会员凭证可带，会员身份同样由 {@code memberId} 显式传参。
 *
 * <p>积分接口的重复调用按成功返回：member 侧的幂等由流水表的唯一索引保证，调用方不必自己去重。
 */
@FeignClient("member")
public interface MemberFeignService {

    /**
     * 查询指定会员的全部收货地址。
     *
     * <p>member 侧不做归属校验，调用方必须自己确认 {@code memberId} 是当前会员，
     * 否则会读到别人的地址。
     *
     * @param memberId 会员 id，不能为 {@code null}
     * @return 该会员的全部收货地址，没有地址时为空列表；本接口不套 {@code R} 外壳，直接返回数组
     */
    @GetMapping(value = "/member/memberreceiveaddress/{memberId}/address")
    List<MemberAddressVo> getAddress(@PathVariable("memberId") Long memberId);

    /**
     * 在会员服务新增一条收货地址。
     *
     * <p>member 侧按入参里的 {@code memberId} 落库，并把新地址强制置为默认地址。
     *
     * @param memberAddressVo 地址内容，必须带 {@code memberId}；不能为 {@code null}
     * @return 统一响应；{@code data} 为新增后的地址（含落库生成的 id），调用方只用 {@code code} 判断是否成功
     */
    @PostMapping("/member/memberreceiveaddress/addLocation")
    R<MemberAddressVo> addLocation(@RequestBody MemberAddressVo memberAddressVo);

    /**
     * 查询会员当前的积分余额。
     *
     * <p>不能用登录凭证里的积分代替：那是登录那一刻的快照，下过单之后就不再是当前余额。
     *
     * @param memberId 会员 id，不能为 {@code null}
     * @return 统一响应；{@code code} 为 0 时 {@code data} 是可用积分（最小为 0），
     *         {@code code} 非 0 时 {@code data} 为 {@code null}
     */
    @GetMapping("/member/trade/integration/balance")
    R<Integer> integrationBalance(@RequestParam("memberId") Long memberId);

    /**
     * 下单抵扣积分。
     *
     * <p>调用前必须已经按本单上限校验过数量：本接口只认余额，不重新判断该单能用多少。
     *
     * @param memberId 会员 id，不能为 {@code null}
     * @param orderSn 订单号，不能为 {@code null}；幂等判据
     * @param points 抵扣数量，必须为正数
     * @return 统一响应；{@code code} 非 0 表示余额不足（15009）或会员不存在（15002）
     */
    @PostMapping("/member/trade/integration/deduct")
    R<Void> deductIntegration(@RequestParam("memberId") Long memberId,
                              @RequestParam("orderSn") String orderSn,
                              @RequestParam("points") Integer points);

    /**
     * 关单时退回已抵扣的积分。
     *
     * <p>幂等且不报错：关单消息重投、以及本来就没用积分的订单都按正常情况处理。
     *
     * @param memberId 会员 id，不能为 {@code null}
     * @param orderSn 订单号，不能为 {@code null}
     * @param points 退回数量，必须为正数
     * @return 统一响应
     */
    @PostMapping("/member/trade/integration/refund")
    R<Void> refundIntegration(@RequestParam("memberId") Long memberId,
                              @RequestParam("orderSn") String orderSn,
                              @RequestParam("points") Integer points);

    /**
     * 支付成功时赠送积分。
     *
     * <p>幂等：支付回调重复投递时，已经赠送过的订单再次调用不会重复入账。
     *
     * @param memberId 会员 id，不能为 {@code null}
     * @param orderSn 订单号，不能为 {@code null}
     * @param points 赠送数量，必须为正数
     * @return 统一响应
     */
    @PostMapping("/member/trade/integration/grant")
    R<Void> grantIntegration(@RequestParam("memberId") Long memberId,
                             @RequestParam("orderSn") String orderSn,
                             @RequestParam("points") Integer points);

}
