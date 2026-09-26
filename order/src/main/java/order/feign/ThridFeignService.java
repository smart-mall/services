package order.feign;

import order.vo.PayVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;


@FeignClient("third-party")
public interface ThridFeignService {

    @GetMapping(value = "/pay",consumes = "application/json")
    String pay(@RequestBody PayVo vo);

}
