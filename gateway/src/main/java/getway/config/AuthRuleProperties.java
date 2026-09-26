package getway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 网关鉴权配置，绑定 {@code gl.auth} 前缀。
 *
 * <p>路径规则写死在 {@link getway.filter.AuthFilter} 里，这里只承载例外清单。
 */
@Data
@Component
@ConfigurationProperties(prefix = "gl.auth")
public class AuthRuleProperties {

    /** 不需要任何凭证的接口，写客户端看到的完整路径（带 {@code /api} 前缀，支持 Ant 通配）。 */
    private List<String> anonymousPaths = new ArrayList<>();

}
