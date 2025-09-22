package thirdParty.controller;

import common.utils.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import thirdParty.utils.MinIOUtil;
import io.minio.http.Method;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/thirdParty/minio")
@Slf4j
public class MinIOController {
    private final MinIOUtil minIOUtil;

    public MinIOController(MinIOUtil minIOUtil) {
        this.minIOUtil = minIOUtil;
    }

    /**
     * 生成单个文件的PUT预签名URL（用于前端直传）
     */
    @PutMapping("/put")
    public R generatePresignedPutUrl(
            @RequestParam String bucketName,
            @RequestParam String objectName,
            @RequestParam(required = false, defaultValue = "604800") int expiryTime) {
        log.info("生成单个文件的PUT预签名URL,{},{}", bucketName, objectName);
        try {
            String url = minIOUtil.generatePresignedPutUrl(bucketName, objectName, expiryTime);
            String fileType = minIOUtil.getFileType(objectName);
            return Objects.requireNonNull(R.ok().put("url", url)).put("type", fileType);
        } catch (Exception e) {
            return R.error(e.getMessage());
        }
    }

    /**
     * 生成单个文件的GET预签名URL（用于前端下载）
     */
    @GetMapping("/get")
    public R generatePresignedGetUrl(
            @RequestParam String bucketName,
            @RequestParam String objectName,
            @RequestParam(required = false, defaultValue = "604800") int expiryTime) {
        log.info("生成单个文件的GET预签名URL,{},{}", bucketName, objectName);
        try {
            String url = minIOUtil.generatePresignedGetUrl(bucketName, objectName, expiryTime);

            return R.ok().put("url", url);
        } catch (Exception e) {
            return R.error(e.getMessage());
        }
    }

    /**
     * 生成单个文件的DELETE预签名URL（用于前端删除）
     */
    @DeleteMapping("/delete")
    public R generatePresignedDeleteUrl(
            @RequestParam String bucketName,
            @RequestParam String objectName,
            @RequestParam(required = false, defaultValue = "3600") int expiryTime) {
        log.info("生成单个文件的DELETE预签名URL,{},{}", bucketName, objectName);
        try {
            String url = minIOUtil.generatePresignedDeleteUrl(bucketName, objectName, expiryTime);
            return R.ok().put("url", url);
        } catch (Exception e) {
            return R.error(e.getMessage());
        }
    }

    /**
     * 批量生成预签名URL
     */
    @PostMapping("/batch")
    public R generateBatchPresignedUrls(
            @RequestParam String bucketName,
            @RequestParam String method,
            @RequestParam(required = false, defaultValue = "604800") int expiryTime,
            @RequestBody List<String> objectNames) {
        log.info("批量生成预签名URL,{},{},{}", bucketName, method, objectNames);
        try {
            Method httpMethod = Method.valueOf(method.toUpperCase());
            Map<String, String> urlMap = minIOUtil.generateBatchPresignedUrls(
                    bucketName, objectNames, httpMethod, expiryTime);
            return R.ok().put("urls", urlMap);
        } catch (IllegalArgumentException e) {
            return R.error("不支持的HTTP方法: " + method);
        } catch (Exception e) {
            return R.error(e.getMessage());
        }
    }

    /**
     * 验证预签名URL是否有效
     */
    @GetMapping("/validate")
    public R validatePresignedUrl(@RequestParam String presignedUrl) {
        log.info("验证预签名URL是否有效,{}", presignedUrl);
        try {
            boolean isValid = minIOUtil.validatePresignedUrl(presignedUrl);
            return Objects.requireNonNull(R.ok()
                            .put("valid", isValid))
                    .put("message", isValid ? "URL有效" : "URL无效或已过期");
        } catch (Exception e) {
            return R.error(e.getMessage());
        }
    }

    /**
     * 获取预签名URL的过期时间
     */
    @GetMapping("/expiry")
    public R getPresignedUrlExpiry(@RequestParam String presignedUrl) {
        try {
            long expiryTime = minIOUtil.getPresignedUrlExpiry(presignedUrl);
            if (expiryTime == -1) {
                return Objects.requireNonNull(R.ok().put("message", "无法解析过期时间")).put("expiryTime", null);
            }
            return Objects.requireNonNull(R.ok()
                            .put("expiryTime", expiryTime))
                    .put("message", "URL过期时间（毫秒时间戳）");
        } catch (Exception e) {
            return R.error(e.getMessage());
        }
    }
}