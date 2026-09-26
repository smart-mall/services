package order.feign;

import common.utils.R;
import order.vo.MemberAddressVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;


@FeignClient("member")
public interface MemberFeignService {

    /**
     * 查询当前用户的全部收货地址
     * @param memberId
     * @return
     */
    @GetMapping(value = "/member/memberreceiveaddress/{memberId}/address")
    List<MemberAddressVo> getAddress(@PathVariable("memberId") Long memberId);

    @PostMapping("/member/memberreceiveaddress/addLocation")
    public R<Void> addLocation(@RequestBody MemberAddressVo memberAddressVo);

}
