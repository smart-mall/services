package member.web;

import common.utils.R;
import member.service.MemberService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 会员数据的服务间查询接口，供 coupon 的生日赠券任务取"今天过生日的会员"。
 *
 * <p>路径不在 {@code /front} 下，调用方是服务而不是浏览器：走 {@code @FeignClient("member")} 直连，
 * 不经网关，没有会员凭证可带。经网关进来的 {@code /member/trade/**} 既不属于前台公开路径也不属于
 * {@code /front/jwt}，会按管理端接口要求管理端凭证，普通会员调不到。
 *
 * <p>本类无状态、线程安全：只持有一个构造器注入的 service。
 */
@RestController
@RequestMapping("member/trade/member")
public class MemberTradeController {

    private final MemberService memberService;

    /**
     * 注入会员账号服务。
     *
     * @param memberService 会员账号服务
     */
    public MemberTradeController(MemberService memberService) {
        this.memberService = memberService;
    }

    /**
     * 查询今天生日的会员 id。
     *
     * @return 会员 id 列表；今天没有人过生日时为空列表
     */
    @GetMapping("/birthday-members")
    public R<List<Long>> birthdayMembers() {
        return R.ok(memberService.listBirthdayMemberIds());
    }

}
