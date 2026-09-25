package getway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/** 网关鉴权的配置。路径规则本身写死在 {@link getway.filter.AuthFilter} 里，这里只有例外清单。 */
@Data
@Component
@ConfigurationProperties(prefix = "gl.auth")
public class AuthRuleProperties {

    /** 不需要任何凭证的接口，写客户端看到的完整路径（带 /api，支持 Ant 通配） */
    private List<String> anonymousPaths = new ArrayList<>();

}
