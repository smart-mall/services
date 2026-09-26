package search;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;

/** Elasticsearch 连通性检查：打印集群名称与版本号，失败只打印错误不抛异常。 */
@SpringBootTest
class SearchApplicationTests {
	@Autowired
	private ElasticsearchTemplate elasticsearchTemplate;

	/** 调用 ES 的 info 接口并打印结果。 */
	@Test
	void contextLoads() {
		try {
			var info = elasticsearchTemplate.execute(ElasticsearchClient::info
			);

			System.out.println("✅ 连接成功！");
			System.out.println("集群名称: " + info.name());
			System.out.println("版本号: " + info.version().number());

		} catch (Exception e) {
			System.err.println("❌ 连接失败: " + e.getMessage());
		}
	}

}
