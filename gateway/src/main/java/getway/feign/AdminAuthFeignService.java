package getway.feign;

import common.utils.R;
import getway.vo.AdminVerifyVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

/** 校验管理端 token。路径要连 renren-fast 的 context-path 一起写。 */
@FeignClient(name = "renren-fast")
public interface AdminAuthFeignService {

    @GetMapping("/renren-fast/sys/usertoken/verify")
    R<AdminVerifyVo> verify(@RequestHeader("token") String token);

}
