package thirdParty.utils;

import com.j256.simplemagic.ContentInfoUtil;
import io.minio.*;
import io.minio.http.Method;
import io.minio.messages.DeleteObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import thirdParty.exception.MinIOException;

import java.io.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * @author 彭超
 * @version 1.0
 * @description MinIO文件系统工具类
 * @date 2025-09-07 18:10
 */
@Component
public class MinIOUtil {

    private static final Logger logger = LoggerFactory.getLogger(MinIOUtil.class);

    private final MinioClient minioClient;

    public MinIOUtil(MinioClient minioClient) {
        this.minioClient = minioClient;
    }

    /**
     * @description 判断桶是否存在
     */
    public boolean isBucketExists(String bucketName) {
        try {
            return minioClient.bucketExists(
                    BucketExistsArgs.builder()
                            .bucket(bucketName)
                            .build()
            );
        } catch (Exception e) {
            throw new MinIOException("检查桶是否存在失败: " + e.getMessage());
        }
    }

    /**
     * @description 判断桶里是否有文件
     */
    public boolean isBucketEmpty(String bucketName) {
        // 先检查桶是否存在
        if (!isBucketExists(bucketName)) {
            throw new MinIOException("桶不存在: " + bucketName);
        }

        try {
            // 列举桶中的对象，最多返回1个（只要有一个就说明不为空）
            var iterator = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(bucketName)
                            .maxKeys(1)  // 只需要检查是否有至少一个对象
                            .build()
            ).iterator();

            // 如果迭代器没有下一个元素，说明桶为空
            return !iterator.hasNext();
        } catch (Exception e) {
            logger.error("判断存储桶是否为空时发生错误: {}", e.getMessage());
            return false;  // 发生异常时默认返回false
        }
    }

    /**
     * @description 判断文件是否存在
     */
    public boolean isFileExists(String bucketName, String objectName) {
        // 先检查桶是否存在，如果桶不存在则直接返回false
        if (!isBucketExists(bucketName)) {
            throw new MinIOException("桶不存在: " + bucketName);
        }

        try {
            // 通过获取文件元数据来判断文件是否存在
            minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build()
            );
            // 执行到这里说明文件存在
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * @description 判断文件夹是否存在
     */
    public boolean isFolderExists(String bucketName, String folderPath) {
        // 先检查桶是否存在
        if (!isBucketExists(bucketName)) {
            throw new MinIOException("桶不存在: " + bucketName);
        }

        // 规范化文件夹路径（确保以 '/' 结尾）
        String normalizedPath;
        if (folderPath.isEmpty()) {
            normalizedPath = "";
        } else {
            String trimmed = folderPath.trim().replaceAll("^/+|/+$", "");
            normalizedPath = trimmed.isEmpty() ? "" : trimmed + "/";
        }

        try {
            // 列举该路径下的对象，最多返回1个（只要有一个就说明文件夹存在）
            var iterator = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(bucketName)
                            .prefix(normalizedPath) // 只列举该路径下的对象
                            .maxKeys(1)             // 只需要检查是否有至少一个对象
                            .recursive(false)       // 不递归，只检查当前层级
                            .build()
            ).iterator();

            // 如果迭代器有下一个元素，说明文件夹存在
            return iterator.hasNext();
        } catch (Exception e) {
            logger.error("判断文件夹是否存在时发生错误: {}", e.getMessage());
            return false;
        }
    }

    /**
     * @description 创建桶
     */
    public void createBucket(String bucketName) {
        if (isBucketExists(bucketName)) {
            throw new MinIOException("桶已存在: " + bucketName);
        }

        try {
            minioClient.makeBucket(
                    MakeBucketArgs.builder()
                            .bucket(bucketName)
                            .build()
            );
        } catch (Exception e) {
            throw new MinIOException("创建桶失败: " + e.getMessage());
        }
    }

    /**
     * @description 删除桶
     */
    public void deleteBucket(String bucketName) {
        if (!isBucketEmpty(bucketName)) {
            throw new MinIOException("禁止删除非空的桶: " + bucketName);
        }
        try {
            minioClient.removeBucket(
                    RemoveBucketArgs.builder()
                            .bucket(bucketName)
                            .build()
            );
        } catch (Exception e) {
            throw new MinIOException("删除桶失败: " + e.getMessage());
        }
    }

    /**
     * @param objectName 对象完整路径（如："docs/reports/file.pdf"）
     * @return Pair<目录路径, 文件后缀>（如：Pair("docs/reports", ".pdf")）
     * @description 解析对象路径，提取目录路径和文件后缀
     */
    private Pair<String, String> parseObjectPath(String objectName) {
        // 查找最后一个斜杠的位置
        int lastSlashIndex = objectName.lastIndexOf('/');

        // 查找最后一个点号的位置（在文件名部分）
        int lastDotIndex = objectName.lastIndexOf('.');

        String directoryPath = (lastSlashIndex == -1) ? "" : objectName.substring(0, lastSlashIndex);
        String fileExtension = (lastDotIndex > lastSlashIndex) ? objectName.substring(lastDotIndex) : "";

        return new Pair<>(directoryPath, fileExtension);
    }

    /**
     * @description 获取桶中对象数量
     */
    private int getBucketObjectCount(String bucketName) {
        int count = 0;
        try {
            var objects = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(bucketName)
                            .recursive(true)
                            .build()
            );
            for (var ignored : objects) {
                count++;
            }
        } catch (Exception e) {
            logger.error("获取桶对象数量失败: {}", e.getMessage());
        }
        return count;
    }

    /**
     * @param oldBucketName 原桶名称
     * @param newBucketName 新桶名称
     * @throws MinIOException 当源桶不存在、目标桶已存在或重命名失败时抛出
     * @description 重命名桶
     */
    public void renameBucket(String oldBucketName, String newBucketName) {
        if (!isBucketExists(oldBucketName)) {
            throw new MinIOException("源桶不存在: " + oldBucketName);
        }

        if (isBucketExists(newBucketName)) {
            throw new MinIOException("目标桶已存在: " + newBucketName);
        }

        // 检查原桶是否为空
        if (!isBucketEmpty(oldBucketName)) {
            throw new MinIOException("禁止重命名非空的桶: " + oldBucketName);
        }

        try {
            // 1. 创建新桶
            minioClient.makeBucket(
                    MakeBucketArgs.builder()
                            .bucket(newBucketName)
                            .build()
            );
            logger.info("新桶创建成功: {}", newBucketName);

            // 2. 删除旧桶
            minioClient.removeBucket(
                    RemoveBucketArgs.builder()
                            .bucket(oldBucketName)
                            .build()
            );
            logger.info("旧桶删除成功: {}", oldBucketName);

            logger.info("桶重命名成功: {} -> {}", oldBucketName, newBucketName);

        } catch (Exception e) {
            // 如果创建新桶成功但删除旧桶失败，尝试清理新桶
            if (isBucketExists(newBucketName)) {
                try {
                    if (isBucketEmpty(newBucketName)) {
                        minioClient.removeBucket(
                                RemoveBucketArgs.builder()
                                        .bucket(newBucketName)
                                        .build()
                        );
                        logger.warn("已清理创建的新桶: {}", newBucketName);
                    }
                } catch (Exception cleanupException) {
                    logger.error("清理新桶失败: {}", cleanupException.getMessage());
                }
            }
            throw new MinIOException("重命名桶失败: " + e.getMessage());
        }
    }

    /**
     * @description 重命名桶（包含内容迁移，适用于非空桶）
     */
    public void renameBucketWithContent(String oldBucketName, String newBucketName) {
        if (!isBucketExists(oldBucketName)) {
            throw new MinIOException("源桶不存在: " + oldBucketName);
        }

        if (isBucketExists(newBucketName)) {
            throw new MinIOException("目标桶已存在: " + newBucketName);
        }

        try {
            // 1. 创建新桶
            createBucket(newBucketName);

            // 2. 复制所有内容到新桶（保持目录结构）
            if (!isBucketEmpty(oldBucketName)) {
                copyFolder(oldBucketName, "", newBucketName, "");
                logger.info("内容复制完成: {} -> {}", oldBucketName, newBucketName);
            }

            // 3. 验证复制结果
            int oldBucketFileCount = getBucketObjectCount(oldBucketName);
            int newBucketFileCount = getBucketObjectCount(newBucketName);

            if (oldBucketFileCount != newBucketFileCount) {
                throw new MinIOException("复制过程中文件数量不一致: " + oldBucketFileCount + " -> " + newBucketFileCount);
            }

            // 4. 删除旧桶（先清空再删除）
            if (!isBucketEmpty(oldBucketName)) {
                deleteFolder(oldBucketName, ""); // 删除根目录下所有文件
            }
            deleteBucket(oldBucketName);

            logger.info("桶重命名完成: {} -> {}, 迁移文件数: {}",
                    oldBucketName, newBucketName, oldBucketFileCount);

        } catch (Exception e) {
            // 错误处理：清理新创建的空桶
            if (isBucketExists(newBucketName) && isBucketEmpty(newBucketName)) {
                try {
                    deleteBucket(newBucketName);
                } catch (Exception cleanupException) {
                    logger.error("清理新桶失败: {}", cleanupException.getMessage());
                }
            }
            throw new MinIOException("重命名桶失败: " + e.getMessage());
        }
    }

    /**
     * @description 获取文件类型
     */
    public String getFileType(String filePath) {
        // 先获取扩展名
        String extension = filePath.substring(filePath.lastIndexOf(".") + 1);
        // 获取文件类型
        try {
            var findExtensionMatch = ContentInfoUtil.findExtensionMatch(extension);
            return (findExtensionMatch != null) ? findExtensionMatch.getMimeType() : "application/octet-stream";
        } catch (Exception e) {
            return "application/octet-stream";
        }
    }

    /**
     * 上传文件到MinIO
     *
     * @param bucketName 桶名称
     * @param file       文件路径
     * @param objectName 目标对象地址（包含路径的文件名）
     */
    public void uploadFile(String bucketName, String file, String objectName) {
        if (!isBucketExists(bucketName)) {
            throw new MinIOException("桶不存在: " + bucketName);
        }

        File localFile = new File(file);
        if (!localFile.exists()) {
            throw new MinIOException("文件不存在: " + file);
        }

        String mimeType = getFileType(file);

        try {
            UploadObjectArgs args = UploadObjectArgs.builder()
                    .bucket(bucketName)
                    .filename(file)
                    .object(objectName)
                    .contentType(mimeType)
                    .build();
            minioClient.uploadObject(args);
        } catch (Exception e) {
            throw new MinIOException("上传文件失败: " + e.getMessage());
        }
    }

    /**
     * @param bucketName 目标存储桶名称
     * @param folderPath 本地文件夹路径（绝对路径）
     * @param folder     桶内目标目录（如："docs/reports"，为空则上传到桶根目录）
     * @throws MinIOException 当文件夹不存在或上传失败时抛出
     * @description 上传本地文件夹到MinIO指定存储桶的指定目录下，保持原目录结构
     */
    public void uploadFolder(String bucketName, String folderPath, String folder) {
        File localFolder = new File(folderPath);

        // 检查本地文件夹是否存在
        if (!localFolder.exists() || !localFolder.isDirectory()) {
            throw new MinIOException("本地文件夹不存在或不是目录: " + folderPath);
        }

        // 检查存储桶是否存在，不存在则创建
        if (!isBucketExists(bucketName)) {
            throw new MinIOException("存储桶不存在: " + bucketName);
        }

        // 处理目标目录路径（确保格式正确，不包含首尾多余分隔符）
        String targetDir = (folder != null) ? folder.trim().replaceAll("^/+|/+$", "") : "";

        try {
            // 递归上传文件夹内容到指定目录
            uploadDirectoryToTarget(
                    bucketName,
                    localFolder,
                    localFolder,
                    targetDir
            );
            logger.info("文件夹上传完成: {} -> {}/{}", folderPath, bucketName, targetDir);
        } catch (Exception e) {
            throw new MinIOException("文件夹上传失败: " + e.getMessage());
        }
    }

    public void uploadFolder(String bucketName, String folderPath) {
        uploadFolder(bucketName, folderPath, "");
    }

    /**
     * 递归上传目录中的所有文件和子目录到指定目标目录
     *
     * @param bucketName    目标存储桶名称
     * @param rootFolder    本地根文件夹（用于计算相对路径）
     * @param currentFolder 当前要处理的本地文件夹
     * @param targetDir     桶内目标目录（可为null，表示根目录）
     */
    private void uploadDirectoryToTarget(
            String bucketName,
            File rootFolder,
            File currentFolder,
            String targetDir
    ) {
        File[] files = currentFolder.listFiles();
        if (files == null) return;

        for (File file : files) {
            // 计算本地文件相对根文件夹的路径
            Path relativePath = rootFolder.toPath().relativize(file.toPath());
            String relativePathStr = relativePath.toString().replace(File.separator, "/");

            // 构建MinIO中的完整对象路径（目标目录 + 相对路径）
            String minioObjectName;
            if (targetDir == null || targetDir.isEmpty()) {
                minioObjectName = relativePathStr;
            } else {
                minioObjectName = targetDir + "/" + relativePathStr;
            }

            if (file.isDirectory()) {
                // 递归处理子目录
                uploadDirectoryToTarget(bucketName, rootFolder, file, targetDir);
            } else {
                try {
                    // 上传文件到MinIO的指定路径
                    minioClient.uploadObject(
                            UploadObjectArgs.builder()
                                    .bucket(bucketName)
                                    .object(minioObjectName)
                                    .filename(file.getAbsolutePath())
                                    .build()
                    );
                    logger.debug("已上传: {}", minioObjectName);
                } catch (Exception e) {
                    logger.error("上传文件失败: {}, 错误: {}", minioObjectName, e.getMessage());
                }
            }
        }
    }

    /**
     * 从MinIO删除文件
     *
     * @param bucketName 桶名称
     * @param objectName 要删除的对象地址（包含路径的文件名）
     */
    public void deleteFile(String bucketName, String objectName) {
        if (!isFileExists(bucketName, objectName)) {
            throw new MinIOException("文件不存在: " + objectName);
        }

        try {
            RemoveObjectArgs args = RemoveObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .build();
            minioClient.removeObject(args);
        } catch (Exception e) {
            throw new MinIOException("删除文件失败: " + e.getMessage());
        }
    }

    /**
     * @param bucketName 桶名称
     * @param folderPath 要删除的文件夹路径（例如："docs/reports"）
     * @throws MinIOException 当桶不存在或删除失败时抛出
     * @description 删除MinIO中的文件夹（包含所有子文件和子目录）
     */
    public void deleteFolder(String bucketName, String folderPath) {
        // 检查桶是否存在
        if (!isBucketExists(bucketName)) {
            throw new MinIOException("存储桶不存在: " + bucketName);
        }

        // 规范化文件夹路径（确保以 '/' 结尾，避免误删同名文件）
        String normalizedPath;
        if (folderPath.isEmpty()) {
            normalizedPath = "";
        } else {
            String trimmed = folderPath.trim().replaceAll("^/+|/+$", "");
            normalizedPath = trimmed.isEmpty() ? "" : trimmed + "/";
        }

        try {
            // 列举该文件夹下的所有对象（递归包含子目录）
            var objects = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(bucketName)
                            .prefix(normalizedPath) // 只列举该路径下的对象
                            .recursive(true)       // 递归处理子目录
                            .build()
            );

            // 收集所有要删除的对象
            List<DeleteObject> deleteList = new ArrayList<>();
            for (var result : objects) {
                deleteList.add(new DeleteObject(result.get().objectName()));
            }

            if (deleteList.isEmpty()) {
                // 这里改为返回而不是抛出异常，因为空文件夹也是正常情况
                logger.info("文件夹为空或不存在: {}", folderPath);
                return;
            }

            // 批量删除对象
            var results = minioClient.removeObjects(
                    RemoveObjectsArgs.builder()
                            .bucket(bucketName)
                            .objects(deleteList)
                            .build()
            );

            // 处理删除结果
            for (var deleteResult : results) {
                try {
                    // 可以在这里处理删除结果
                } catch (Exception e) {
                    logger.error("删除失败: {}", e.getMessage());
                }
            }

            logger.info("文件夹删除完成: {}，成功删除 {} 个对象", folderPath, deleteList.size());

        } catch (Exception e) {
            throw new MinIOException("删除文件夹失败: " + e.getMessage());
        }
    }

    /**
     * @param sourceBucketName 源存储桶名称
     * @param sourceObjectName 源对象名称（包含路径）
     * @param targetBucketName 目标存储桶名称
     * @param targetObjectName 目标对象名称（包含路径）
     * @throws MinIOException 当源文件不存在、目标桶不存在或复制失败时抛出
     * @description 桶间复制文件
     */
    public void copyFile(
            String sourceBucketName,
            String sourceObjectName,
            String targetBucketName,
            String targetObjectName
    ) {
        // 检查源文件是否存在
        if (!isFileExists(sourceBucketName, sourceObjectName)) {
            throw new MinIOException("源文件不存在: " + sourceBucketName + "/" + sourceObjectName);
        }

        // 检查目标桶是否存在
        if (!isBucketExists(targetBucketName)) {
            throw new MinIOException("目标存储桶不存在: " + targetBucketName);
        }

        if (isFileExists(targetBucketName, targetObjectName)) {
            throw new MinIOException("目标文件已存在: " + targetBucketName + "/" + targetObjectName);
        }

        try {
            // 使用 MinIO 的 copyObject 方法进行复制
            minioClient.copyObject(
                    CopyObjectArgs.builder()
                            .source(
                                    CopySource.builder()
                                            .bucket(sourceBucketName)
                                            .object(sourceObjectName)
                                            .build()
                            )
                            .bucket(targetBucketName)
                            .object(targetObjectName)
                            .build()
            );

            logger.info("文件复制成功: {}/{} -> {}/{}",
                    sourceBucketName, sourceObjectName, targetBucketName, targetObjectName);
        } catch (Exception e) {
            throw new MinIOException("文件复制失败: " + e.getMessage());
        }
    }

    /**
     * @param sourceBucketName 源存储桶名称
     * @param sourceFolderPath 源文件夹路径（例如："docs/reports"）
     * @param targetBucketName 目标存储桶名称
     * @param targetFolderPath 目标文件夹路径（例如："backup/reports"）
     * @throws MinIOException 当源文件夹不存在、目标桶不存在或复制失败时抛出
     * @description 桶间复制文件夹
     */
    public void copyFolder(
            String sourceBucketName,
            String sourceFolderPath,
            String targetBucketName,
            String targetFolderPath
    ) {
        if (!isFolderExists(sourceBucketName, sourceFolderPath)) {
            throw new MinIOException("源文件夹不存在: " + sourceBucketName + "/" + sourceFolderPath);
        }

        // 检查目标桶是否存在
        if (!isBucketExists(targetBucketName)) {
            throw new MinIOException("目标存储桶不存在: " + targetBucketName);
        }

        // 判断目标文件夹是否存在
        if (isFolderExists(targetBucketName, targetFolderPath)) {
            throw new MinIOException("目标文件夹已存在: " + targetBucketName + "/" + targetFolderPath);
        }

        // 规范化源文件夹路径（确保以 '/' 结尾）
        String normalizedSourcePath;
        if (sourceFolderPath.isEmpty()) {
            normalizedSourcePath = "";
        } else {
            String trimmed = sourceFolderPath.trim().replaceAll("^/+|/+$", "");
            normalizedSourcePath = trimmed.isEmpty() ? "" : trimmed + "/";
        }

        // 规范化目标文件夹路径（确保不以 '/' 开头，以 '/' 结尾）
        String normalizedTargetPath;
        if (targetFolderPath.isEmpty()) {
            normalizedTargetPath = "";
        } else {
            String trimmed = targetFolderPath.trim().replaceAll("^/+|/+$", "");
            normalizedTargetPath = trimmed.isEmpty() ? "" : trimmed + "/";
        }

        try {
            // 列举源文件夹下的所有对象（递归包含子目录）
            var objects = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(sourceBucketName)
                            .prefix(normalizedSourcePath)
                            .recursive(true)
                            .build()
            );

            int copiedCount = 0;
            int failedCount = 0;

            // 遍历所有对象并进行复制
            for (var result : objects) {
                try {
                    String sourceObjectName = result.get().objectName();

                    // 计算目标对象名称（保持相对路径结构）
                    String relativePath;
                    if (normalizedSourcePath.isEmpty()) {
                        relativePath = sourceObjectName;
                    } else {
                        relativePath = sourceObjectName.substring(normalizedSourcePath.length());
                    }

                    String targetObjectName;
                    if (normalizedTargetPath.isEmpty()) {
                        targetObjectName = relativePath;
                    } else {
                        targetObjectName = normalizedTargetPath + relativePath;
                    }

                    // 复制文件
                    minioClient.copyObject(
                            CopyObjectArgs.builder()
                                    .source(
                                            CopySource.builder()
                                                    .bucket(sourceBucketName)
                                                    .object(sourceObjectName)
                                                    .build()
                                    )
                                    .bucket(targetBucketName)
                                    .object(targetObjectName)
                                    .build()
                    );

                    copiedCount++;
                    logger.debug("复制文件: {} -> {}", sourceObjectName, targetObjectName);

                } catch (Exception e) {
                    failedCount++;
                    logger.error("复制文件失败: {}, 错误: {}", result.get().objectName(), e.getMessage());
                }
            }

            if (copiedCount == 0 && failedCount == 0) {
                throw new MinIOException("源文件夹为空或不存在: " + sourceBucketName + "/" + sourceFolderPath);
            } else {
                logger.info("文件夹复制完成: {}/{} -> {}/{}, 成功: {}, 失败: {}",
                        sourceBucketName, sourceFolderPath, targetBucketName, targetFolderPath,
                        copiedCount, failedCount);
            }

        } catch (Exception e) {
            throw new MinIOException("复制文件夹失败: " + e.getMessage());
        }
    }

    /**
     * @description 桶间移动文件
     */
    public void moveFile(
            String sourceBucketName,
            String sourceObjectName,
            String targetBucketName,
            String targetObjectName
    ) {
        // 先复制文件到目标位置
        copyFile(sourceBucketName, sourceObjectName, targetBucketName, targetObjectName);

        // 复制成功后删除源文件
        deleteFile(sourceBucketName, sourceObjectName);

        logger.info("文件移动成功: {}/{} -> {}/{}",
                sourceBucketName, sourceObjectName, targetBucketName, targetObjectName);
    }

    /**
     * @description 桶间移动文件夹
     */
    public void moveFolder(
            String sourceBucketName,
            String sourceFolderPath,
            String targetBucketName,
            String targetFolderPath
    ) {
        // 先复制文件夹到目标位置
        copyFolder(sourceBucketName, sourceFolderPath, targetBucketName, targetFolderPath);

        // 复制成功后删除源文件夹
        deleteFolder(sourceBucketName, sourceFolderPath);

        logger.info("文件夹移动成功: {}/{} -> {}/{}",
                sourceBucketName, sourceFolderPath, targetBucketName, targetFolderPath);
    }

    /**
     * @description 桶内复制文件
     */
    public void copyFile(String bucketName, String sourceObjectName, String targetObjectName) {
        copyFile(bucketName, sourceObjectName, bucketName, targetObjectName);
    }

    /**
     * @description 桶内复制文件夹
     */
    public void copyFolder(String bucketName, String sourceFolderPath, String targetFolderPath) {
        copyFolder(bucketName, sourceFolderPath, bucketName, targetFolderPath);
    }

    /**
     * @description 桶内移动文件
     */
    public void moveFile(String bucketName, String sourceObjectName, String targetObjectName) {
        moveFile(bucketName, sourceObjectName, bucketName, targetObjectName);
    }

    /**
     * @description 桶内移动文件夹
     */
    public void moveFolder(String bucketName, String sourceFolderPath, String targetFolderPath) {
        moveFolder(bucketName, sourceFolderPath, bucketName, targetFolderPath);
    }

    /**
     * 从MinIO下载文件
     *
     * @param bucketName 桶名称
     * @param objectName 要下载的对象地址（包含路径的文件名）
     */
    public InputStream downloadFile(String bucketName, String objectName) {
        if (!isFileExists(bucketName, objectName)) {
            throw new MinIOException("文件不存在: " + bucketName + "/" + objectName);
        }

        try {
            GetObjectArgs args = GetObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .build();
            return minioClient.getObject(args);
        } catch (Exception e) {
            throw new MinIOException("下载文件失败: " + e.getMessage());
        }
    }

    /**
     * @param bucketName 存储桶名称
     * @param objectName 原对象完整路径（包含路径和文件名，如："docs/reports/old-file.pdf"）
     * @param newName    新文件名（不包含路径和后缀，如："new-file"）
     * @throws MinIOException 当原文件不存在、新文件已存在或重命名失败时抛出
     * @description 重命名文件（保持路径和后缀不变，只修改文件名）
     */
    public void renameFile(String bucketName, String objectName, String newName) {
        // 解析原文件路径，提取目录路径和文件后缀
        var pathInfo = parseObjectPath(objectName);
        String directoryPath = pathInfo.getFirst();
        String fileExtension = pathInfo.getSecond();

        // 构建新对象的完整路径
        String newObjectName;
        if (directoryPath.isEmpty()) {
            newObjectName = newName + fileExtension;
        } else {
            newObjectName = directoryPath + "/" + newName + fileExtension;
        }

        moveFile(bucketName, objectName, newObjectName);
    }

    /**
     * @param bucketName    存储桶名称
     * @param oldFolderPath 原文件夹完整路径（包含路径和文件夹名，如："docs/old-folder"）
     * @param newName       新文件夹名（不包含路径，如："new-folder"）
     * @description 重命名文件夹
     */
    public void renameFolder(String bucketName, String oldFolderPath, String newName) {
        // 解析原文件夹路径，提取父目录路径
        var pathInfo = parseObjectPath(oldFolderPath);
        String parentPath = pathInfo.getFirst();

        // 构建新文件夹的完整路径
        String newFolderPath;
        if (parentPath.isEmpty()) {
            newFolderPath = newName;
        } else {
            newFolderPath = parentPath + "/" + newName;
        }

        moveFolder(bucketName, oldFolderPath, newFolderPath);
    }

    /**
     * @param bucketName 存储桶名称
     * @param folderPath 文件夹路径（例如："docs/images"）
     * @return 随机文件的完整路径，如果文件夹为空或不存在则返回null
     * @throws MinIOException 当桶不存在或读取文件列表失败时抛出
     * @description 随机从文件夹中选取一个文件
     */
    public String getRandomFileFromFolder(String bucketName, String folderPath) {
        // 检查文件夹是否存在
        if (!isFolderExists(bucketName, folderPath)) {
            throw new MinIOException("文件夹不存在: " + bucketName + "/" + folderPath);
        }

        // 规范化文件夹路径（确保以 '/' 结尾）
        String normalizedPath;
        if (folderPath.isEmpty()) {
            normalizedPath = "";
        } else {
            String trimmed = folderPath.trim().replaceAll("^/+|/+$", "");
            normalizedPath = trimmed.isEmpty() ? "" : trimmed + "/";
        }

        try {
            // 获取文件夹下的所有文件（递归包含子目录）
            List<String> fileList = new ArrayList<>();
            var objects = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(bucketName)
                            .prefix(normalizedPath)
                            .recursive(true)
                            .build()
            );

            // 收集所有文件路径（排除文件夹）
            for (var result : objects) {
                String objectName = result.get().objectName();
                // 排除文件夹（MinIO中文件夹是以/结尾的对象）
                if (!objectName.endsWith("/") && !objectName.equals(normalizedPath)) {
                    fileList.add(objectName);
                }
            }

            // 检查是否找到文件
            if (fileList.isEmpty()) {
                throw new MinIOException("文件夹为空: " + bucketName + "/" + folderPath);
            }

            // 随机选择一个文件
            Random random = new Random();
            String randomFile = fileList.get(random.nextInt(fileList.size()));
            logger.debug("从文件夹 {} 中随机选择文件: {} (总共 {} 个文件)",
                    folderPath, randomFile, fileList.size());

            return randomFile;

        } catch (Exception e) {
            throw new MinIOException("获取文件夹文件列表失败: " + e.getMessage());
        }
    }

    public String getRandomFileFromFolder(String bucketName) {
        return getRandomFileFromFolder(bucketName, "");
    }

    /**
     * @param bucketName 存储桶名称
     * @param filePath   文件路径（包含文件名，如："docs/empty.txt"）
     * @return 创建成功的文件信息（虚拟File对象，实际操作在MinIO中完成）
     * @throws MinIOException 当桶不存在或创建失败时抛出
     * @description 创建空文件（上传一个空内容的对象）
     */
    public File createEmptyFile(String bucketName, String filePath) {
        // 检查文件是否已存在
        if (isFileExists(bucketName, filePath)) {
            throw new MinIOException("文件已存在: " + bucketName + "/" + filePath);
        }

        try {
            // 创建临时空文件
            File tempFile = File.createTempFile("minio_empty", ".tmp");
            tempFile.deleteOnExit(); // 程序退出时删除临时文件

            // 上传空文件到MinIO
            minioClient.uploadObject(
                    UploadObjectArgs.builder()
                            .bucket(bucketName)
                            .object(filePath)
                            .filename(tempFile.getAbsolutePath())
                            .contentType("application/octet-stream") // 通用二进制类型
                            .build()
            );

            logger.info("空文件创建成功: {}/{}", bucketName, filePath);

            // 返回虚拟的File对象（实际文件在MinIO中）
            return new File(filePath);

        } catch (Exception e) {
            throw new MinIOException("创建空文件失败: " + e.getMessage());
        }
    }

    /**
     * @param bucketName 桶名称
     * @param objectName 要下载的对象地址（包含路径的文件名）
     * @return 本地临时File对象
     * @throws MinIOException 当文件不存在或下载失败时抛出
     * @description 从MinIO下载文件到本地临时文件并返回File对象
     */
    public File downloadToFile(String bucketName, String objectName) {
        // 检查文件是否存在
        if (!isFileExists(bucketName, objectName)) {
            throw new MinIOException("文件不存在: " + bucketName + "/" + objectName);
        }

        try {
            // 提取文件名和扩展名
            String fileName = objectName.substring(objectName.lastIndexOf('/') + 1);
            String fileExtension = fileName.contains(".") ?
                    fileName.substring(fileName.lastIndexOf('.') + 1) : "";

            // 创建临时文件，保留原文件扩展名
            String prefix = "minio_download_";
            String suffix = fileExtension.isEmpty() ? "" : "." + fileExtension;
            File tempFile = File.createTempFile(prefix, suffix);
            tempFile.deleteOnExit(); // 程序退出时删除临时文件

            // 下载文件内容
            InputStream inputStream = downloadFile(bucketName, objectName);

            // 将内容写入临时文件
            try (OutputStream output = new FileOutputStream(tempFile)) {
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    output.write(buffer, 0, bytesRead);
                }
            }

            logger.info("文件下载成功: {}/{} -> {}", bucketName, objectName, tempFile.getAbsolutePath());
            return tempFile;

        } catch (Exception e) {
            throw new MinIOException("下载文件失败: " + e.getMessage());
        }
    }

    /**
     * @param bucketName 存储桶名称
     * @param objectName 文件对象路径
     * @return 文件内容字符串
     * @throws MinIOException 当文件不存在或读取失败时抛出
     * @description 读取文件内容为字符串
     */
    public String readFileToString(String bucketName, String objectName) {
        // 检查文件是否存在
        if (!isFileExists(bucketName, objectName)) {
            throw new MinIOException("文件不存在: " + bucketName + "/" + objectName);
        }

        try {
            // 下载文件
            InputStream inputStream = downloadFile(bucketName, objectName);

            // 读取文件内容为字符串
            StringBuilder content = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line).append("\n");
                }
            }

            logger.debug("文件读取成功: {}/{}, 内容长度: {}", bucketName, objectName, content.length());
            return content.toString();

        } catch (Exception e) {
            throw new MinIOException("读取文件失败: " + e.getMessage());
        }
    }

    /**
     * @param bucketName  存储桶名称
     * @param objectName  文件对象路径
     * @param content     要写入的内容
     * @param contentType 文件内容类型（默认为text/plain）
     * @throws MinIOException 当写入失败时抛出
     * @description 向文件写入字符串（会覆盖原有内容）
     */
    public void writeStringToFile(String bucketName, String objectName, String content, String contentType) {
        // 先检查桶是否存在
        if (!isBucketExists(bucketName)) {
            throw new MinIOException("桶不存在: " + bucketName);
        }

        try {
            // 创建临时文件并写入内容
            File tempFile = File.createTempFile("minio_write", ".tmp");
            tempFile.deleteOnExit();

            // 写入内容到临时文件
            try (FileWriter writer = new FileWriter(tempFile, StandardCharsets.UTF_8)) {
                writer.write(content);
            }

            // 上传文件到MinIO（会覆盖已存在的文件）
            minioClient.uploadObject(
                    UploadObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .filename(tempFile.getAbsolutePath())
                            .contentType(contentType != null ? contentType : "text/plain")
                            .build()
            );

            logger.info("文件写入成功: {}/{}, 内容长度: {}", bucketName, objectName, content.length());

        } catch (Exception e) {
            throw new MinIOException("写入文件失败: " + e.getMessage());
        }
    }

    public void writeStringToFile(String bucketName, String objectName, String content) {
        writeStringToFile(bucketName, objectName, content, "text/plain");
    }

    /**
     * @param bucketName     存储桶名称
     * @param folderPath     文件夹路径（例如："docs/images"）
     * @param recursive      是否递归获取子文件夹中的文件（默认false）
     * @param includeFolders 是否包含文件夹路径（默认false）
     * @return 文件路径列表
     * @throws MinIOException 当桶不存在或读取失败时抛出
     * @description 获取目标文件夹下的所有文件路径
     */
    public List<String> getFilePathsFromFolder(
            String bucketName,
            String folderPath,
            boolean recursive,
            boolean includeFolders
    ) {
        // 检查文件夹是否存在
        if (!isFolderExists(bucketName, folderPath)) {
            throw new MinIOException("文件夹不存在: " + bucketName + "/" + folderPath);
        }

        // 规范化文件夹路径（确保以 '/' 结尾）
        String normalizedPath;
        if (folderPath.isEmpty()) {
            normalizedPath = "";
        } else {
            String trimmed = folderPath.trim().replaceAll("^/+|/+$", "");
            normalizedPath = trimmed.isEmpty() ? "" : trimmed + "/";
        }

        try {
            List<String> filePaths = new ArrayList<>();

            // 列举文件夹下的所有对象
            var objects = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(bucketName)
                            .prefix(normalizedPath)
                            .recursive(recursive)
                            .build()
            );

            // 收集文件路径
            for (var result : objects) {
                String objectName = result.get().objectName();

                // 排除文件夹本身（如果查询的是根目录，需要排除空字符串）
                if (!objectName.equals(normalizedPath)) {
                    boolean isFolder = objectName.endsWith("/");

                    if (includeFolders && isFolder) {
                        // 包含文件夹
                        filePaths.add(objectName);
                    } else if (!isFolder) {
                        // 只包含文件
                        filePaths.add(objectName);
                    }
                }
            }

            logger.debug("从文件夹 {} 获取到 {} 个文件路径{}{}",
                    folderPath, filePaths.size(),
                    recursive ? " (递归)" : "",
                    includeFolders ? " (包含文件夹)" : "");

            filePaths.sort(String::compareTo); // 返回排序后的路径列表
            return filePaths;

        } catch (Exception e) {
            throw new MinIOException("获取文件夹文件列表失败: " + e.getMessage());
        }
    }

    public List<String> getFilePathsFromFolder(String bucketName, String folderPath) {
        return getFilePathsFromFolder(bucketName, folderPath, true, false);
    }

    public List<String> getFilePathsFromFolder(String bucketName, String folderPath, boolean recursive) {
        return getFilePathsFromFolder(bucketName, folderPath, recursive, false);
    }

    /**
     * @param bucketName 存储桶名称
     * @param folderPath 文件夹路径（默认为根目录）
     * @param recursive  是否递归获取子文件夹中的文件（true）
     * @param reverse    是否倒序返回 （默认true）
     * @return 分割后的路径字符数组列表，按路径深度倒序排列
     * @throws MinIOException 当桶不存在或读取失败时抛出
     * @description 获取目标文件夹下的所有文件路径的分割为路径字符数组（倒序返回）
     */
    public List<List<String>> getFilePathsFromFolderSplit(
            String bucketName,
            String folderPath,
            boolean recursive,
            boolean reverse
    ) {
        // 获取所有文件路径（递归）
        List<String> allFilePaths = getFilePathsFromFolder(bucketName, folderPath, recursive, false);

        // 分割路径并倒序排列
        List<List<String>> result = new ArrayList<>();
        for (String filePath : allFilePaths) {
            String[] parts = filePath.split("/");
            List<String> partList = new ArrayList<>();
            for (String part : parts) {
                if (!part.isEmpty()) {
                    partList.add(part);
                }
            }
            if (reverse) {
                List<String> reversed = new ArrayList<>();
                for (int i = partList.size() - 1; i >= 0; i--) {
                    reversed.add(partList.get(i));
                }
                result.add(reversed);
            } else {
                result.add(partList);
            }
        }

        // 按路径深度倒序排序
        result.sort((a, b) -> Integer.compare(b.size(), a.size()));
        return result;
    }

    public List<List<String>> getFilePathsFromFolderSplit(String bucketName, String folderPath) {
        return getFilePathsFromFolderSplit(bucketName, folderPath, true, true);
    }

    public List<List<String>> getFilePathsFromFolderSplit(String bucketName, String folderPath, boolean recursive) {
        return getFilePathsFromFolderSplit(bucketName, folderPath, recursive, true);
    }

    // Pair 辅助类
    public static class Pair<F, S> {
        private final F first;
        private final S second;

        public Pair(F first, S second) {
            this.first = first;
            this.second = second;
        }

        public F getFirst() {
            return first;
        }

        public S getSecond() {
            return second;
        }
    }

    /**
     * @param bucketName 存储桶名称
     * @param objectName 对象名称（包含路径）
     * @param expiryTime 过期时间（单位：秒，默认7天）
     * @return 预签名 URL
     * @throws MinIOException 当生成失败时抛出
     * @description 生成用于前端直传的预签名 PUT URL
     */
    public String generatePresignedPutUrl(String bucketName, String objectName, int expiryTime) {
        if (!isBucketExists(bucketName)) {
            throw new MinIOException("存储桶不存在: " + bucketName);
        }

        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.PUT)
                            .bucket(bucketName)
                            .object(objectName)
                            .expiry(expiryTime, TimeUnit.SECONDS)
                            .build()
            );
        } catch (Exception e) {
            throw new MinIOException("生成预签名 PUT URL 失败: " + e.getMessage());
        }
    }

    public String generatePresignedPutUrl(String bucketName, String objectName) {
        // 默认过期时间：7天
        return generatePresignedPutUrl(bucketName, objectName, 60 * 60 * 24 * 7);
    }

    /**
     * @param bucketName 存储桶名称
     * @param objectName 对象名称（包含路径）
     * @param expiryTime 过期时间（单位：秒，默认7天）
     * @return 预签名 URL
     * @throws MinIOException 当生成失败时抛出
     * @description 生成用于前端下载的预签名 GET URL
     */
    public String generatePresignedGetUrl(String bucketName, String objectName, int expiryTime) {
        if (!isFileExists(bucketName, objectName)) {
            throw new MinIOException("文件不存在: " + bucketName + "/" + objectName);
        }

        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucketName)
                            .object(objectName)
                            .expiry(expiryTime, TimeUnit.SECONDS)
                            .build()
            );
        } catch (Exception e) {
            throw new MinIOException("生成预签名 GET URL 失败: " + e.getMessage());
        }
    }

    public String generatePresignedGetUrl(String bucketName, String objectName) {
        // 默认过期时间：7天
        return generatePresignedGetUrl(bucketName, objectName, 60 * 60 * 24 * 7);
    }

    /**
     * @param bucketName 存储桶名称
     * @param objectName 对象名称（包含路径）
     * @param expiryTime 过期时间（单位：秒，默认1小时）
     * @return 预签名 URL
     * @throws MinIOException 当生成失败时抛出
     * @description 生成用于前端删除的预签名 DELETE URL
     */
    public String generatePresignedDeleteUrl(String bucketName, String objectName, int expiryTime) {
        if (!isFileExists(bucketName, objectName)) {
            throw new MinIOException("文件不存在: " + bucketName + "/" + objectName);
        }

        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.DELETE)
                            .bucket(bucketName)
                            .object(objectName)
                            .expiry(expiryTime, TimeUnit.SECONDS)
                            .build()
            );
        } catch (Exception e) {
            throw new MinIOException("生成预签名 DELETE URL 失败: " + e.getMessage());
        }
    }

    public String generatePresignedDeleteUrl(String bucketName, String objectName) {
        // 默认过期时间：1小时（删除操作应该更短的时间）
        return generatePresignedDeleteUrl(bucketName, objectName, 60 * 60);
    }

    /**
     * @param bucketName  存储桶名称
     * @param objectNames 对象名称列表
     * @param method      HTTP 方法（PUT/GET/DELETE）
     * @param expiryTime  过期时间（单位：秒）
     * @return 对象名称到预签名 URL 的映射
     * @throws MinIOException 当生成失败时抛出
     * @description 批量生成预签名 URL
     */
    public Map<String, String> generateBatchPresignedUrls(String bucketName, List<String> objectNames,
                                                          Method method, int expiryTime) {
        if (!isBucketExists(bucketName)) {
            throw new MinIOException("存储桶不存在: " + bucketName);
        }

        Map<String, String> result = new HashMap<>();
        for (String objectName : objectNames) {
            try {
                String url = minioClient.getPresignedObjectUrl(
                        GetPresignedObjectUrlArgs.builder()
                                .method(method)
                                .bucket(bucketName)
                                .object(objectName)
                                .expiry(expiryTime, TimeUnit.SECONDS)
                                .build()
                );
                result.put(objectName, url);
            } catch (Exception e) {
                logger.error("为对象 {} 生成预签名 URL 失败: {}", objectName, e.getMessage());
                result.put(objectName, null);
            }
        }
        return result;
    }

    /**
     * @param presignedUrl 预签名 URL
     * @return 是否有效
     * @description 验证预签名 URL 是否有效
     */
    public boolean validatePresignedUrl(String presignedUrl) {
        try {
            // 尝试发起 HEAD 请求验证 URL 是否有效
            java.net.URL url = new java.net.URL(presignedUrl);
            java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
            connection.setRequestMethod("HEAD");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            int responseCode = connection.getResponseCode();
            return responseCode != 403 && responseCode != 404; // 不是禁止或未找到就认为有效
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * @param presignedUrl 预签名 URL
     * @return 过期时间戳（毫秒），如果无法解析返回 -1
     * @description 获取预签名 URL 的过期时间
     */
    public long getPresignedUrlExpiry(String presignedUrl) {
        try {
            java.net.URL url = new java.net.URL(presignedUrl);
            String query = url.getQuery();
            if (query != null) {
                String[] params = query.split("&");
                for (String param : params) {
                    if (param.startsWith("X-Amz-Expires=")) {
                        String expiresStr = param.substring("X-Amz-Expires=".length());
                        long expiresSeconds = Long.parseLong(expiresStr);
                        // 假设 URL 是刚生成的，计算过期时间戳
                        return System.currentTimeMillis() + (expiresSeconds * 1000);
                    }
                }
            }
            return -1;
        } catch (Exception e) {
            return -1;
        }
    }

    /**
     * 从MinIO删除文件
     *
     * @param http https地址
     */
    public void deleteFile(String http) {
        try {
            // 1. 解析 URL，提取路径部分
            URI uri = new URI(http);
            String path = uri.getPath();  // /gulimall/2026-5-15/xxx.png

            // 2. 去掉开头的 /
            String bucketAndObject = path.substring(1);

            // 3. 分离 bucket 和 objectName
            int firstSlash = bucketAndObject.indexOf('/');
            String bucket = bucketAndObject.substring(0, firstSlash);
            String objectName = bucketAndObject.substring(firstSlash + 1);

            deleteFile(bucket, objectName);
        } catch (Exception e) {
            throw new MinIOException("删除文件失败: " + e.getMessage());
        }
    }

}