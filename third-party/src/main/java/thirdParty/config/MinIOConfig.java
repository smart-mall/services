package thirdParty.config;

import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MinIO 客户端配置：按 {@code minio.*} 配置项构建全局唯一的 {@link MinioClient}。
 *
 * <p>这里只读 {@code serverPoint}，即后端 SDK 连 MinIO 的地址；拼给前端的 URL 前缀由
 * {@link MediaProperties#getClientPoint()} 提供。
 */
@Configuration
public class MinIOConfig {

    @Value("${minio.serverPoint}")
    private String endpoint;

    @Value("${minio.access-key}")
    private String accessKey;

    @Value("${minio.secret-key}")
    private String secretKey;

    /**
     * 构建 MinIO 客户端，endpoint 与凭据分别取自 {@code minio.serverPoint}、
     * {@code minio.access-key}、{@code minio.secret-key}。
     *
     * <p>构建过程不发起网络请求，连通性问题在首次调用时才暴露。
     *
     * @return 全局唯一的 MinIO 客户端
     */
    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }
}