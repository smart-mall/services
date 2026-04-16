package search;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;

@SpringBootTest
class SearchApplicationTests {
	@Autowired
	private ElasticsearchTemplate elasticsearchTemplate;

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
