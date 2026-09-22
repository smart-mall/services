package thirdParty.service.impl;

import common.exception.BaseCodeEnum;
import common.exception.BaseException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import thirdParty.config.MediaProperties;
import thirdParty.exception.MinIOException;
import thirdParty.service.MediaService;
import thirdParty.utils.MinIOUtil;
import thirdParty.vo.MediaFileVo;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 媒体文件业务实现：key 生成、类型校验、URL 拼装、删除解析。
 *
 * @see MediaService
 */
@Slf4j
@Service
public class MediaServiceImpl implements MediaService {

    private final MinIOUtil minIOUtil;
    private final MediaProperties properties;

    public MediaServiceImpl(MinIOUtil minIOUtil, MediaProperties properties) {
        this.minIOUtil = minIOUtil;
        this.properties = properties;
    }

    @Override
    public MediaFileVo upload(MultipartFile file) {
        return store(read(file));
    }

    @Override
    public List<MediaFileVo> uploadBatch(MultipartFile[] files) {
        if (files == null || files.length == 0) {
            throw new BaseException(BaseCodeEnum.MEDIA_FILE_EMPTY);
        }

        // 第一遍只读不写：任何一个文件不合法都在这里抛出去，此时桶里还是干净的
        List<Pending> pending = new ArrayList<>(files.length);
        for (MultipartFile file : files) {
            pending.add(read(file));
        }

        // 第二遍才真正写对象
        List<MediaFileVo> result = new ArrayList<>(pending.size());
        for (Pending item : pending) {
            result.add(store(item));
        }
        return result;
    }

    @Override
    public void delete(String url) {
        String key = resolveKey(url);
        if (key == null) {
            throw new BaseException(BaseCodeEnum.MEDIA_URL_INVALID, "文件地址不合法: " + url);
        }
        minIOUtil.removeObject(properties.getBucket(), key);
    }

    @Override
    public List<String> deleteBatch(List<String> urls) {
        List<String> failed = new ArrayList<>();
        if (urls == null || urls.isEmpty()) {
            return failed;
        }

        for (String url : urls) {
            String key = resolveKey(url);
            if (key == null) {
                log.warn("批量删除跳过无法识别的文件地址: {}", url);
                failed.add(url);
                continue;
            }
            try {
                minIOUtil.removeObject(properties.getBucket(), key);
            } catch (MinIOException e) {
                log.warn("批量删除失败 url={}: {}", url, e.getMessage());
                failed.add(url);
            }
        }
        return failed;
    }

    // ------------------------------------------------------------------
    // 内部
    // ------------------------------------------------------------------

    /** 一批文件在写对象之前的中间态 */
    private record Pending(byte[] content, ImageFormat format, String originalName) {
    }

    /**
     * 校验并读出文件内容。
     *
     * <p>这里把整份内容读进堆，是有意的取舍：单文件上限 10MB（{@code minio.max-size}，
     * multipart 那一层也挡了一遍），调用方是后台管理端的人工上传，一次几十张，总量有界。
     * 换来的是<b>可以在写任何对象之前把"非空、大小、真实格式"三项校验完</b>，
     * 从而做到整批成功或整批失败。</p>
     *
     * <p>如果以后要接高频 UGC，这里得换成流式（{@code file.getInputStream()} 直接交给
     * MinIO），代价是校验只能放到写之后，并且需要引入两阶段确认来防孤儿对象。</p>
     */
    private Pending read(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BaseException(BaseCodeEnum.MEDIA_FILE_EMPTY);
        }
        if (file.getSize() > properties.getMaxSize().toBytes()) {
            throw new BaseException(BaseCodeEnum.MEDIA_FILE_TOO_LARGE);
        }

        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException e) {
            log.error("读取上传文件失败 name={}", file.getOriginalFilename(), e);
            throw new MinIOException("读取上传文件失败，请稍后重试");
        }

        ImageFormat format = ImageFormat.detect(content);
        if (format == null) {
            // 不回显探测结果（"你传的其实是 application/x-php"那类），
            // 那等于白送一个内容指纹接口。只说支持什么。
            throw new BaseException(BaseCodeEnum.MEDIA_FILE_TYPE_NOT_SUPPORTED);
        }
        return new Pending(content, format, file.getOriginalFilename());
    }

    private MediaFileVo store(Pending pending) {
        String key = buildKey(pending.format());
        minIOUtil.putObject(properties.getBucket(), key,
                new ByteArrayInputStream(pending.content()), pending.content().length,
                pending.format().contentType);
        return new MediaFileVo(buildUrl(key), pending.originalName(), pending.content().length);
    }

    /**
     * 生成 object key：{@code {年}/{月}/{日}/{uuid}.{扩展名}}。
     *
     * <p>日期和文件名都用服务端的。改造前这两段都是前端产出的 —— {@code new Date()} 取的
     * 是客户端时钟和时区，系统时间不准就写到别的"目录"；{@code getUUID()} 是
     * {@code Math.random()} 拼的；扩展名来自原始文件名。三者都是调用方说了算。</p>
     */
    private String buildKey(ImageFormat format) {
        LocalDate today = LocalDate.now();
        return "%d/%02d/%02d/%s.%s".formatted(
                today.getYear(), today.getMonthValue(), today.getDayOfMonth(),
                UUID.randomUUID().toString().replace("-", ""),
                format.extension);
    }

    private String buildUrl(String key) {
        return urlPrefix() + key;
    }

    /**
     * 把库里存的完整 URL 还原成 object key。
     *
     * <p>只认 {@code clientPoint/bucket/} 这个前缀。对不上的（历史遗留的外站地址、
     * 手填的随便一个 URL）返回 null —— 这条判断同时回答了"这个地址是不是我们自己签发的"，
     * 所以在删除路径上不能省。</p>
     */
    private String resolveKey(String url) {
        if (!StringUtils.hasText(url)) {
            return null;
        }
        String prefix = urlPrefix();
        if (!url.startsWith(prefix)) {
            return null;
        }
        String key = url.substring(prefix.length());
        return key.isBlank() ? null : key;
    }

    /** {@code clientPoint} 末尾的斜杠可有可无，统一归一化后再拼，避免出现双斜杠 */
    private String urlPrefix() {
        String base = properties.getClientPoint();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + "/" + properties.getBucket() + "/";
    }

    /**
     * 允许的图片格式。
     *
     * <p>按<b>文件头魔数</b>判定，不看扩展名、也不看客户端发来的 Content-Type ——
     * 那两个都是调用方随便写的，而改造前的 {@code getFileType} 正是拿扩展名去查 MIME 表，
     * 等于"内容到底是什么"完全没验。</p>
     */
    private enum ImageFormat {

        JPEG("image/jpeg", "jpg"),
        PNG("image/png", "png"),
        GIF("image/gif", "gif"),
        WEBP("image/webp", "webp");

        final String contentType;
        final String extension;

        ImageFormat(String contentType, String extension) {
            this.contentType = contentType;
            this.extension = extension;
        }

        static ImageFormat detect(byte[] content) {
            if (startsWith(content, 0xFF, 0xD8, 0xFF)) {
                return JPEG;
            }
            // PNG 的 8 字节签名，后 4 字节 \r\n\x1a\n 用来检测传输层做过换行转换
            if (startsWith(content, 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A)) {
                return PNG;
            }
            if (startsWith(content, 'G', 'I', 'F', '8')
                    && (at(content, 4) == '7' || at(content, 4) == '9')
                    && at(content, 5) == 'a') {
                return GIF;
            }
            // WEBP 是 RIFF 容器：0-3 是 "RIFF"，8-11 是 "WEBP"
            if (startsWith(content, 'R', 'I', 'F', 'F')
                    && at(content, 8) == 'W' && at(content, 9) == 'E'
                    && at(content, 10) == 'B' && at(content, 11) == 'P') {
                return WEBP;
            }
            return null;
        }

        private static boolean startsWith(byte[] content, int... magic) {
            if (content.length < magic.length) {
                return false;
            }
            for (int i = 0; i < magic.length; i++) {
                if ((content[i] & 0xFF) != (magic[i] & 0xFF)) {
                    return false;
                }
            }
            return true;
        }

        private static int at(byte[] content, int index) {
            return index < content.length ? content[index] & 0xFF : -1;
        }
    }
}
