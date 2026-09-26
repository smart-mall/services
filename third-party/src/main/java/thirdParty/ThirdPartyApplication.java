package thirdParty;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * third-party 服务启动类。
 *
 * <p>对外提供地址树查询、短信与邮件验证码发送、MinIO 文件管理，并消费商品删除事件清理孤儿文件。
 */
@SpringBootApplication
public class ThirdPartyApplication {

	public static void main(String[] args) {
		SpringApplication.run(ThirdPartyApplication.class, args);
	}

}
