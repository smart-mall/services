package getway.feign;

import common.utils.R;
import getway.vo.AdminVerifyVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * 管理端凭证校验的远程调用接口，指向 renren-fast 的 token 校验端点。
 *
 * <p>路径要连 renren-fast 的 context-path 一起写，Feign 不会自动补这个前缀。
 */
@FeignClient(name = "renren-fast")
public interface AdminAuthFeignService {

    /**
     * 校验管理端 token 并返回管理员身份。
     *
     * @param token 管理端登录凭证，通过 {@code token} 请求头传递，不能为空
     * @return 校验结果；token 无效时 {@code data} 为 {@code null}
     */
    @GetMapping("/renren-fast/sys/usertoken/verify")
    R<AdminVerifyVo> verify(@RequestHeader("token") String token);

}
