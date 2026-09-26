package seckill.feign;

import common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import seckill.vo.SeckillSessionWithSkusVo;
/**
 * 远程调用coupon优惠服务
 */
@FeignClient("coupon")
public interface CouponFeignService {

    @GetMapping(value = "/coupon/seckillsession/Lates3DaySession")
    R<List<SeckillSessionWithSkusVo>> getLates3DaySession();

}
