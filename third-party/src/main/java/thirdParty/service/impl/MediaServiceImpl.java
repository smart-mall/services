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

    /** {@inheritDoc} */
    @Override
    public MediaFileVo upload(MultipartFile file) {
        return store(read(file));
    }

    /** {@inheritDoc} */
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

        // 全部校验通过后才写对象，保证整批原子
        List<MediaFileVo> result = new ArrayList<>(pending.size());
        for (Pending item : pending) {
            result.add(store(item));
        }
        return result;
    }

    /** {@inheritDoc} */
    @Override
    public void delete(String url) {
        String key = resolveKey(url);
        if (key == null) {
            throw new BaseException(BaseCodeEnum.MEDIA_URL_INVALID, "文件地址不合法: " + url);
        }
        minIOUtil.removeObject(properties.getBucket(), key);
    }

    /** {@inheritDoc} */
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


    /** 已通过校验、尚未写入对象存储的文件，暂存内容与目标格式。 */
    private record Pending(byte[] content, ImageFormat format, String originalName) {
    }

    /**
     * 校验并读出文件内容。
     *
     * <p>整份内容读进堆是有意取舍：单文件上限 10MB（{@code minio.max-size}，multipart 那一层
     * 也挡了一遍），换来的是能在写任何对象前把"非空、大小、真实格式"三项校验完，
     * 从而做到整批成功或整批失败。</p>
     *
     * <p>若采用流式（{@code file.getInputStream()} 直接交给 MinIO），校验只能放到写之后，
     * 并且需要两阶段确认来防孤儿对象。</p>
     *
     * @param file 上传的文件，不能为 {@code null}
     * @return 校验通过的中间态，含完整内容、识别出的格式与原始文件名
     * @throws BaseException 文件为空、超出大小上限或格式不被支持时抛出
     * @throws MinIOException 读取文件内容失败时抛出
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

    /**
     * 把中间态写入对象存储，并组装成对外返回结果。
     *
     * @param pending 已通过校验的文件
     * @return 上传结果，地址由 {@code clientPoint} 前缀拼出
     */
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
     * <p>日期取服务端时钟，扩展名取识别出的真实格式，均不采用调用方提供的文件名或客户端时钟，
     * 避免存储路径被外部指定。</p>
     *
     * @param format 已识别的图片格式，决定扩展名
     * @return object key，不含 bucket 前缀
     */
    private String buildKey(ImageFormat format) {
        LocalDate today = LocalDate.now();
        return "%d/%02d/%02d/%s.%s".formatted(
                today.getYear(), today.getMonthValue(), today.getDayOfMonth(),
                UUID.randomUUID().toString().replace("-", ""),
                format.extension);
    }

    /**
     * 用对外前缀拼出完整可访问地址。
     *
     * @param key object key
     * @return 完整地址，可直接存库
     */
    private String buildUrl(String key) {
        return urlPrefix() + key;
    }

    /**
     * 把库里存的完整 URL 还原成 object key。
     *
     * <p>只认 {@code clientPoint/bucket/} 前缀，对不上的（外站地址、手填的任意 URL）返回
     * {@code null}；这条判断同时回答了"地址是否由本服务签发"，删除路径上不能省。</p>
     *
     * @param url 业务表里存的完整地址
     * @return object key；地址为空、前缀不匹配或 key 为空白时返回 {@code null}
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

    /**
     * 归一化出 URL 前缀：{@code clientPoint} 末尾的斜杠可有可无，统一去掉后再拼，避免双斜杠。
     *
     * @return 形如 {@code {clientPoint}/{bucket}/} 的前缀
     */
    private String urlPrefix() {
        String base = properties.getClientPoint();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + "/" + properties.getBucket() + "/";
    }

    /**
     * 允许上传的图片格式，按文件头魔数判定。
     *
     * <p>不看扩展名、也不看客户端发来的 Content-Type：二者都由调用方提供，
     * 无法证明内容的真实类型。</p>
     */
    private enum ImageFormat {

        /** JPEG：文件头为 {@code FF D8 FF}，扩展名 jpg。 */
        JPEG("image/jpeg", "jpg"),

        /** PNG：8 字节签名为 {@code 89 50 4E 47 0D 0A 1A 0A}，扩展名 png。 */
        PNG("image/png", "png"),

        /** GIF：以 {@code GIF87a} 或 {@code GIF89a} 开头，扩展名 gif。 */
        GIF("image/gif", "gif"),

        /** WEBP：RIFF 容器，偏移 0 为 {@code RIFF}、偏移 8 为 {@code WEBP}，扩展名 webp。 */
        WEBP("image/webp", "webp");

        /** 写入对象存储的 Content-Type。 */
        final String contentType;

        /** object key 使用的扩展名，不带点。 */
        final String extension;

        ImageFormat(String contentType, String extension) {
            this.contentType = contentType;
            this.extension = extension;
        }

        /**
         * 按文件头魔数识别图片格式。
         *
         * @param content 文件内容
         * @return 识别出的格式；不在支持列表内时返回 {@code null}
         */
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

        /** 比较内容开头若干字节；长度不足时返回 {@code false}。 */
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

        /** 读取指定偏移的字节；越界返回 -1，让后续格式判断自然落空。 */
        private static int at(byte[] content, int index) {
            return index < content.length ? content[index] & 0xFF : -1;
        }
    }
}
