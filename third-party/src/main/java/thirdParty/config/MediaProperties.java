package thirdParty.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.unit.DataSize;

/**
 * 媒体模块配置，集中承载 MinIO 的桶名、对外地址前缀与单文件大小上限。
 *
 * <p>{@code serverPoint} 是后端 SDK 连 MinIO 的地址（docker 里是容器名，浏览器解析不了），
 * {@code clientPoint} 是拼给前端的 URL 前缀（必须是浏览器可达的宿主机地址）。拼对外地址只能
 * 走 {@code clientPoint}：SDK endpoint 上的容器名发到浏览器后连不上。</p>
 *
 * @see MinIOConfig
 */
@Getter
@Component
public class MediaProperties {

    /** 对外 URL 前缀，浏览器据此访问对象；末尾斜杠可有可无，拼装时会归一化 */
    @Value("${minio.clientPoint}")
    private String clientPoint;

    /** 对象存储桶名，上传与删除都落在同一个桶里 */
    @Value("${minio.bucket}")
    private String bucket;

    /** 单文件大小上限，需与 {@code spring.servlet.multipart.max-file-size} 保持一致 */
    @Value("${minio.max-size:10MB}")
    private DataSize maxSize;
}
