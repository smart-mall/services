package coupon.feign;

import common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

import java.util.Map;
/**
 * renren-fast 用户服务的 Feign 契约，按用户 id 批量取用户名。
 *
 * <p>返回值统一是 {@link R}：调用方必须先判 {@code code}，为 0 时才能读 {@code data}。
 */
@FeignClient(name = "renren-fast")
public interface RenrenFeignService {

    /**
     * 按用户 id 批量查询用户名。
     *
     * @param userIds 用户 id 集合，不能为 {@code null}
     * @return 统一响应；{@code code} 为 0 时 {@code data} 是 userId 到用户名的映射，查不到的 id 不在映射中
     *         （调用方取值得到 {@code null}）；{@code code} 非 0 时 {@code data} 为 {@code null}
     */
    @PostMapping("/renren-fast/sys/user/getUserNames")
    R<Map<Long, String>> getUserNames(@RequestBody List<Long> userIds);
}
