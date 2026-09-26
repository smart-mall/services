package common.config;

import com.alibaba.csp.sentinel.adapter.spring.webmvc.callback.BlockExceptionHandler;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.fastjson.JSON;
import common.exception.BaseCodeEnum;
import common.utils.R;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Sentinel 限流的兜底响应：被拦截时按统一的 {@link R} 结构返回 {@code 10003}。
 *
 * <p>不带配置前缀，由 common 的 {@code AutoConfiguration.imports} 注册；
 * 限流规则本身不在此配置。
 */
@Component
public class SentinelConfig implements BlockExceptionHandler {

    /**
     * {@inheritDoc}
     *
     * <p>HTTP 状态码保持 200，限流码只放在响应体的 {@code code} 里，字符集固定 UTF-8。
     */
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, BlockException ex) throws IOException {
        R<Void> error = R.error(BaseCodeEnum.TO_MANY_REQUEST);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");
        response.getWriter().write(JSON.toJSONString(error));
    }

}
