package order.feign;

import common.utils.R;
import order.vo.MemberAddressVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;


/** 会员服务的远程调用接口，用于查询与新增收货地址。 */
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

}
