package common.config;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.Configuration;

@Configuration
@FeignClient(name = "common")
public class ApplicationConfig {
}
