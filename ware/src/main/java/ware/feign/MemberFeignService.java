package ware.feign;

import common.utils.R;
import ware.vo.MemberAddressVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;


/**
 * 会员服务的 Feign 客户端：按地址 ID 取收货地址，用于计算运费。
 */
@FeignClient("member")
public interface MemberFeignService {

    /**
     * 查询会员收货地址。
     *
     * @param id 收货地址 ID，不能为 {@code null}
     * @return 收货地址；地址不存在时 {@code data} 为 {@code null}
     */
    @RequestMapping("/member/memberreceiveaddress/info/{id}")
    R<MemberAddressVo> info(@PathVariable("id") Long id);

}
