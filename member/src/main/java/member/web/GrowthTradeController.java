package member.web;

import common.to.MemberGrowthVo;
import common.to.MemberLevelVo;
import common.utils.R;
import member.service.GrowthAccountService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 会员成长值的赠送与查询接口，供 order 在支付成功后赠送、product 在评价成功后赠送，
 * 以及 coupon 判断领券门槛时调用。
 *
 * <p>路径不在 {@code /front} 下，调用方是服务而不是浏览器：都走 {@code @FeignClient("member")} 直连，
 * 不经网关，没有会员凭证可带，所以会员身份由调用方显式传 {@code memberId}，本类不做登录校验。
 *
 * <p>把 {@code memberId} 当成可信入参的前提是这些路径不会由浏览器直接触达。经网关进来的请求里
 * {@code /member/trade/**} 既不属于前台公开路径也不属于 {@code /front/jwt}，
 * 会按管理端接口要求管理端凭证，普通会员调不到。
 *
 * <p>两个赠送接口都不需要调用方自己去重：同一订单的同一类变动重复调用按成功返回，
 * 幂等由流水表的唯一索引 {@code uk_order_change} 保证。
 *
 * <p>本类无状态、线程安全：只持有一个构造器注入的 service。
 */
@RestController
@RequestMapping("member/trade/growth")
public class GrowthTradeController {

    private final GrowthAccountService growthAccountService;

    /**
     * 注入成长值账户服务。
     *
     * @param growthAccountService 成长值账户服务
     */
    public GrowthTradeController(GrowthAccountService growthAccountService) {
        this.growthAccountService = growthAccountService;
    }

    /**
     * 支付成功时赠送成长值。
     *
     * <p>数量由调用方给出：它是下单那一刻按商品原价算定、写在订单上的快照，
     * 不随后续等级或规则变化重算。
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
        growthAccountService.grant(memberId, orderSn, points);

        return R.ok();
    }

    /**
     * 评价成功后赠送成长值。
     *
     * <p>不接收数量：送多少取自会员当前等级配置的每次评价成长值，规则留在会员侧。
     * 会员没有等级、或该等级把这一项配成 0 时按无操作处理，仍应答成功。
     *
     * @param memberId 会员 id，必填
     * @param orderSn 订单号，必填；幂等判据
     * @return 统一成功响应，不含业务数据
     */
    @PostMapping("/comment-grant")
    public R<Void> commentGrant(@RequestParam("memberId") Long memberId,
                                @RequestParam("orderSn") String orderSn) {
        growthAccountService.grantByComment(memberId, orderSn);

        return R.ok();
    }

    /**
     * 查询会员的成长状态：当前成长值、所属等级与该等级的门槛。
     *
     * <p>券服务按本接口判断领券门槛，取的是实时值而不是登录凭证里的快照。
     *
     * @param memberId 会员 id，必填
     * @return 成长状态；会员没有等级时等级相关字段为 {@code null}，成长值归一到 0
     */
    @GetMapping("/level")
    public R<MemberGrowthVo> level(@RequestParam("memberId") Long memberId) {
        return R.ok(growthAccountService.snapshot(memberId));
    }

    /**
     * 查询全部会员等级定义，按成长值门槛升序。
     *
     * <p>券服务按本接口取"券要求的等级对应多少门槛"，再与会员的成长值比较。等级表只有几行。
     *
     * @return 等级定义列表；等级表为空时为空列表
     */
    @GetMapping("/levels")
    public R<List<MemberLevelVo>> levels() {
        return R.ok(growthAccountService.listLevels());
    }

}
