package thirdParty;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import thirdParty.utils.MinIOUtil;

@SpringBootTest
class ThirdPartyApplicationTests {

	@Autowired
	private MinIOUtil minIOUtil;

	@Test
	void contextLoads() {
		// 创建空文件
		minIOUtil.createEmptyFile("text", "te1st.txt");
	}

}
