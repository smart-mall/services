package member.web;

import common.utils.R;
import member.service.IntegrationAccountService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 会员积分的变动接口，供 order 在算价、下单、关单与支付四个时机调用。
 *
 * <p>路径不在 {@code /front} 下，调用方是服务而不是浏览器：order 走
 * {@code @FeignClient("member")} 直连，不经网关，没有会员凭证可带，所以会员身份由调用方显式传
 * {@code memberId}，本类不做登录校验。
 *
 * <p>把 {@code memberId} 当成可信入参的前提是这些路径不会由浏览器直接触达。经网关进来的请求里
 * {@code /member/trade/**} 既不属于前台公开路径也不属于 {@code /front/jwt}，
 * 会按管理端接口要求管理端凭证，普通会员调不到。
 *
 * <p>四个接口都不需要调用方自己去重：同一订单的同一类变动重复调用按成功返回，
 * 幂等由流水表的唯一索引 {@code uk_order_change} 保证。
 *
 * <p>本类无状态、线程安全：只持有一个构造器注入的 service。
 */
@RestController
@RequestMapping("member/trade/integration")
public class IntegrationTradeController {

    private final IntegrationAccountService integrationAccountService;

    /**
     * 注入积分账户服务。
     *
     * @param integrationAccountService 积分账户服务
     */
    public IntegrationTradeController(IntegrationAccountService integrationAccountService) {
        this.integrationAccountService = integrationAccountService;
    }

    /**
     * 查询会员的积分余额。
     *
     * <p>结算页的"可用积分"由 order 经本接口取得，不取登录凭证里的积分：凭证是登录那一刻的快照，
     * 下过单之后就不再是当前余额。
     *
     * @param memberId 会员 id，必填
     * @return 可用积分，最小为 0；积分列为空时返回 0
     */
    @GetMapping("/balance")
    public R<Integer> balance(@RequestParam("memberId") Long memberId) {
        return R.ok(integrationAccountService.balance(memberId));
    }

    /**
     * 下单抵扣积分。
     *
     * <p>调用方必须先按本单上限校验过数量：本接口只认余额，不重新判断该单能用多少。
     *
     * @param memberId 会员 id，必填
     * @param orderSn 订单号，必填；幂等判据与流水的来源凭据
     * @param points 抵扣数量，必须为正数
     * @return 统一成功响应，不含业务数据
     */
    @PostMapping("/deduct")
    public R<Void> deduct(@RequestParam("memberId") Long memberId,
                          @RequestParam("orderSn") String orderSn,
                          @RequestParam("points") Integer points) {
        integrationAccountService.deduct(memberId, orderSn, points);

        return R.ok();
    }

    /**
     * 关单时退回已抵扣的积分。
     *
     * <p>幂等且不报错：关单消息重投、以及本来就没用积分的订单都按正常情况处理。
     *
     * @param memberId 会员 id，必填
     * @param orderSn 订单号，必填
     * @param points 退回数量，必须为正数
     * @return 统一成功响应，不含业务数据
     */
    @PostMapping("/refund")
    public R<Void> refund(@RequestParam("memberId") Long memberId,
                          @RequestParam("orderSn") String orderSn,
                          @RequestParam("points") Integer points) {
        integrationAccountService.refund(memberId, orderSn, points);

        return R.ok();
    }

    /**
     * 支付成功时赠送积分。
     *
     * <p>幂等：支付回调重复投递时，已经赠送过的订单再次调用不会重复入账。
     *
     * @param memberId 会员 id，必填
     * @param orderSn 订单号，必填
     * @param points 赠送数量，必须为正数
     * @return 统一成功响应，不含业务数据
     */
    @PostMapping("/grant")
    public R<Void> grant(@RequestParam("memberId") Long memberId,
                         @RequestParam("orderSn") String orderSn,
                         @RequestParam("points") Integer points) {
        integrationAccountService.grant(memberId, orderSn, points);

        return R.ok();
    }

}
