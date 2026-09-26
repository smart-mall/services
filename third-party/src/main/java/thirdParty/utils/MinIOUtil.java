package thirdParty.utils;

import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import thirdParty.exception.MinIOException;

import java.io.InputStream;

/**
 * MinIO 存储适配层，只提供写对象与删对象两个能力。
 *
 * <p>本层不做业务判断：object key 的生成、类型校验、URL 拼装都在 {@code MediaService} 完成，
 * 因此它不需要知道 bucket 名应该是什么。</p>
 */
@Slf4j
@Component
public class MinIOUtil {

    private final MinioClient minioClient;

    public MinIOUtil(MinioClient minioClient) {
        this.minioClient = minioClient;
    }

    /**
     * 上传对象。
     *
     * <p>用 {@code stream(in, size, -1)} 而不是 {@code filename(...)}：调用方已经持有完整内容，
     * 这里不再落地临时文件。</p>
     *
     * @param bucket      桶名
     * @param objectName  对象 key，由调用方生成
     * @param in          内容流，SDK 会读取完整内容
     * @param size        内容字节数，必须与实际长度一致
     * @param contentType MIME 类型，作为对象的 Content-Type 写入
     * @throws MinIOException MinIO 写入失败时抛出，异常原文只进日志
     */
    public void putObject(String bucket, String objectName, InputStream in, long size, String contentType) {
        try {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectName)
                    .stream(in, size, -1)
                    .contentType(contentType)
                    .build());
        } catch (Exception e) {
            // S3 的异常文本里带 endpoint、bucket 和错误码。这些只进日志：
            // 调用方只需要知道"可以重试"，不需要知道我们的内网长什么样。
            log.error("上传对象失败 bucket={} object={}", bucket, objectName, e);
            throw new MinIOException("文件上传失败，请稍后重试");
        }
    }

    /**
     * 删除对象。
     *
     * <p>S3/MinIO 的 removeObject 对不存在的 key 也返回成功，因此本操作天然幂等，
     * 调用方不需要先判断对象是否存在。</p>
     *
     * @param bucket     桶名
     * @param objectName 对象 key
     * @throws MinIOException MinIO 删除失败时抛出，异常原文只进日志
     */
    public void removeObject(String bucket, String objectName) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectName)
                    .build());
        } catch (Exception e) {
            log.error("删除对象失败 bucket={} object={}", bucket, objectName, e);
            throw new MinIOException("文件删除失败，请稍后重试");
        }
    }
}
