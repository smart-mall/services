package thirdParty.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.unit.DataSize;

/**
 * 媒体模块配置。
 *
 * <p>把 {@code clientPoint} 和 {@code serverPoint} 的职责分开，是这次改造的重点之一：</p>
 * <ul>
 *   <li>{@code serverPoint} —— 后端 SDK 连 MinIO 用的地址。docker 里是容器名
 *       {@code http://gl-minio:9000}，<b>浏览器解析不了</b>。</li>
 *   <li>{@code clientPoint} —— <b>拼给前端的 URL 前缀</b>，必须是浏览器能访问到的地址
 *       （docker 里是宿主机发布的 {@code http://localhost:59000}）。</li>
 * </ul>
 *
 * <p>改造前这两个值是混着用的：{@code MinIOConfig} 只读 {@code serverPoint} 建客户端，
 * 而预签名 URL 的 host 恰恰取自 SDK 的 endpoint —— 所以发到浏览器手里的地址带的是
 * 容器名，PUT 和 GET 都连不上。{@code clientPoint} 当时已经写在配置里，但<b>全仓库
 * 没有任何一行代码读过它</b>：位置留好了，线没接。</p>
 *
 * @see MinIOConfig
 */
@Getter
@Component
public class MediaProperties {

    /** 对外 URL 前缀。末尾带不带斜杠都兼容，拼装时会归一化 */
    @Value("${minio.clientPoint}")
    private String clientPoint;

    @Value("${minio.bucket}")
    private String bucket;

    /** 单文件大小上限。和 third-party 的 spring.servlet.multipart.max-file-size 保持一致 */
    @Value("${minio.max-size:10MB}")
    private DataSize maxSize;
}
