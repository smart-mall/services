package thirdParty.utils;

import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import thirdParty.exception.MinIOException;

import java.io.InputStream;

/**
 * MinIO 存储适配层。只做两件事：写一个对象、删一个对象。
 *
 * <p>改造前这个类有 1448 行，而真正被调用的只有十来个方法。其余是桶重命名
 * （手写"列举全部对象 → 逐个 copy → 删旧桶"）、文件夹递归上传/下载/复制、
 * {@code createEmptyFile}、{@code writeStringToFile}、{@code readFileToString} 这类
 * 和项目无关的能力 —— 更像一份 MinIO SDK 的练习集。桶重命名那段尤其危险：
 * 中途失败会在存储上留下半个桶，而没有任何地方会清理它。</p>
 *
 * <p>现在 key 的生成、类型校验、URL 拼装都在 {@code MediaService}，这一层不参与
 * 任何业务判断，所以它也不再需要知道 bucket 名应该是什么。</p>
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
     * <p>用 {@code stream(in, size, -1)} 而不是 {@code filename(...)}：调用方已经持有
     * 内容（见 {@code MediaServiceImpl#read} 里为什么这么做），这里不再落地临时文件。</p>
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
     * <p>S3/MinIO 的 removeObject 对不存在的 key 也返回成功，所以这个操作天然幂等，
     * 调用方不需要先判断对象在不在（改造前要先 {@code statObject} 再删，多一次 RTT
     * 而且把"权限不足""网络错误"和"对象真不存在"混成了同一个 false）。</p>
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
