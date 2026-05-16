package coupon.fegin;

import common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(name = "renren-fast")
public interface RenrenFeignService {
    @PostMapping("/renren-fast/sys/user/getUserNames")
    R getUserNames(@RequestBody List<Long> userIds);
}
