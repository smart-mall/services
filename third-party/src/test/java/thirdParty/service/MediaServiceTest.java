package thirdParty.service;

import common.exception.BaseCodeEnum;
import common.exception.BaseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartFile;
import thirdParty.config.MediaProperties;
import thirdParty.exception.MinIOException;
import thirdParty.service.impl.MediaServiceImpl;
import thirdParty.utils.MinIOUtil;
import thirdParty.vo.MediaFileVo;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * MediaService 的单元测试。
 *
 * <p>不启动 Spring：被测的都是纯函数（key 生成、魔数校验、URL 拼装、前缀反解），
 * 把存储层换成替身就能完整覆盖，不需要 Nacos / MySQL / MinIO 在场。</p>
 */
class MediaServiceTest {

    private static final String CLIENT_POINT = "http://localhost:59000";
    private static final String BUCKET = "gulimall";

    /** 各格式的真实文件头。后面的字节是随便填的，只要魔数对得上 */
    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00, 0x10, 'J', 'F', 'I', 'F'};
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00};
    private static final byte[] GIF = {'G', 'I', 'F', '8', '9', 'a', 0x01, 0x00};
    private static final byte[] WEBP = {'R', 'I', 'F', 'F', 0x24, 0x00, 0x00, 0x00, 'W', 'E', 'B', 'P', 'V', 'P', '8', ' '};
    private static final byte[] NOT_IMAGE = "<?php echo 'pwned'; ?>".getBytes(StandardCharsets.UTF_8);

    private RecordingMinIOUtil storage;
    private MediaService mediaService;

    @BeforeEach
    void setUp() {
        storage = new RecordingMinIOUtil();
        mediaService = new MediaServiceImpl(storage, propertiesWith(CLIENT_POINT));
    }

    private static MediaProperties propertiesWith(String clientPoint) {
        MediaProperties properties = new MediaProperties();
        ReflectionTestUtils.setField(properties, "clientPoint", clientPoint);
        ReflectionTestUtils.setField(properties, "bucket", BUCKET);
        ReflectionTestUtils.setField(properties, "maxSize", DataSize.ofMegabytes(10));
        return properties;
    }

    /** 一条测试用例：标签 + 文件内容 + 期望的扩展名与 Content-Type。 */
    private record Case(String label, byte[] content, String extension, String contentType) {
    }

    /** 记录一次 putObject 的实参。 */
    private record PutCall(String bucket, String key, String contentType) {
    }

    /**
     * 手写的存储层替身：只记录调用参数，不碰 MinIO。
     *
     * <p>这里不用 mock 框架：被测的都是纯函数，需要观察的只有"传下去的 key 和
     * contentType 是什么"，一个记账用的子类就够了，还省掉了一层字节码增强
     * （Mockito 的 MockMaker 需要动态挂 agent，在受限 JVM 上起不来）。</p>
     */
    private static class RecordingMinIOUtil extends MinIOUtil {

        private final List<PutCall> puts = new ArrayList<>();
        private final List<String> removed = new ArrayList<>();
        /** 设了之后指定 key 的删除会抛异常，用来验证批量删除的容错。 */
        private RuntimeException removeFailure;
        private String removeFailureKey;

        private RecordingMinIOUtil() {
            // 父类的 minioClient 在这条路径上用不到：两个对外方法都被覆盖了
            super(null);
        }

        @Override
        public void putObject(String bucket, String objectName, InputStream in, long size, String contentType) {
            puts.add(new PutCall(bucket, objectName, contentType));
        }

        @Override
        public void removeObject(String bucket, String objectName) {
            if (removeFailure != null && objectName.equals(removeFailureKey)) {
                throw removeFailure;
            }
            removed.add(objectName);
        }

        /** 让指定 key 的删除失败。 */
        private void failRemoveOf(String key) {
            this.removeFailureKey = key;
            this.removeFailure = new MinIOException("boom");
        }
    }

    private void assertNothingStored() {
        assertTrue(storage.puts.isEmpty(), "校验没过时不该写对象");
        assertTrue(storage.removed.isEmpty(), "校验没过时不该删对象");
    }

    @Test
    @DisplayName("四种允许的格式都能识别；key 用服务端的日期+uuid，扩展名来自文件头而不是文件名")
    void uploadDetectsFormatAndBuildsKey() {
        List<Case> cases = List.of(
                new Case("jpeg", JPEG, "jpg", "image/jpeg"),
                new Case("png", PNG, "png", "image/png"),
                new Case("gif", GIF, "gif", "image/gif"),
                new Case("webp", WEBP, "webp", "image/webp"));

        List<MediaFileVo> results = new ArrayList<>();
        for (Case c : cases) {
            // 原始文件名和 Content-Type 都故意写成不对的：这两个值不可信，不能影响落库结果
            results.add(mediaService.upload(
                    new MockMultipartFile("file", "payload.txt", "text/plain", c.content())));
        }

        assertEquals(cases.size(), storage.puts.size());
        for (int i = 0; i < cases.size(); i++) {
            Case c = cases.get(i);
            PutCall put = storage.puts.get(i);

            assertEquals(BUCKET, put.bucket());
            assertEquals(c.contentType(), put.contentType());
            assertTrue(put.key().matches("\\d{4}/\\d{2}/\\d{2}/[0-9a-f]{32}\\." + c.extension()),
                    c.label() + " 的 key 不符合预期: " + put.key());

            assertEquals(CLIENT_POINT + "/" + BUCKET + "/" + put.key(), results.get(i).url());
            assertEquals("payload.txt", results.get(i).name());
            assertEquals(c.content().length, results.get(i).size());
        }
    }

    @Test
    @DisplayName("伪装成图片的脚本被拒：类型按文件头判定，不看扩展名也不看 Content-Type")
    void uploadRejectsNonImage() {
        MultipartFile evil = new MockMultipartFile("file", "evil.jpg", "image/jpeg", NOT_IMAGE);

        BaseException e = assertThrows(BaseException.class, () -> mediaService.upload(evil));

        assertEquals(BaseCodeEnum.MEDIA_FILE_TYPE_NOT_SUPPORTED.getCode(), e.getCode());
        assertNothingStored();
    }

    @Test
    @DisplayName("空文件和超限文件也在写对象之前就被拒")
    void uploadRejectsEmptyAndOversized() {
        BaseException empty = assertThrows(BaseException.class, () -> mediaService.upload(
                new MockMultipartFile("file", "a.jpg", "image/jpeg", new byte[0])));
        assertEquals(BaseCodeEnum.MEDIA_FILE_EMPTY.getCode(), empty.getCode());

        byte[] oversized = new byte[(int) DataSize.ofMegabytes(10).toBytes() + 1];
        System.arraycopy(JPEG, 0, oversized, 0, JPEG.length);
        BaseException tooLarge = assertThrows(BaseException.class, () -> mediaService.upload(
                new MockMultipartFile("file", "a.jpg", "image/jpeg", oversized)));
        assertEquals(BaseCodeEnum.MEDIA_FILE_TOO_LARGE.getCode(), tooLarge.getCode());

        assertNothingStored();
    }

    @Test
    @DisplayName("批量上传整批原子：只要有一个文件不合法，一个对象都不写")
    void uploadBatchIsAllOrNothing() {
        MultipartFile ok = new MockMultipartFile("files", "ok.png", "image/png", PNG);
        MultipartFile bad = new MockMultipartFile("files", "bad.png", "image/png", NOT_IMAGE);

        assertThrows(BaseException.class, () -> mediaService.uploadBatch(new MultipartFile[]{ok, bad}));

        assertNothingStored();
    }

    @Test
    @DisplayName("批量上传全合法时每个文件各写一个对象，扩展名各自独立")
    void uploadBatchStoresEveryFile() {
        MultipartFile png = new MockMultipartFile("files", "a.png", "image/png", PNG);
        MultipartFile gif = new MockMultipartFile("files", "b.gif", "image/gif", GIF);

        List<MediaFileVo> result = mediaService.uploadBatch(new MultipartFile[]{png, gif});

        assertEquals(2, result.size());
        assertEquals(2, storage.puts.size());
        assertTrue(storage.puts.get(0).key().endsWith(".png"));
        assertTrue(storage.puts.get(1).key().endsWith(".gif"));
        assertTrue(result.get(0).url().endsWith(".png"));
        assertTrue(result.get(1).url().endsWith(".gif"));
    }

    @Test
    @DisplayName("批量上传空入参直接报错，不碰存储")
    void uploadBatchRejectsEmptyInput() {
        assertThrows(BaseException.class, () -> mediaService.uploadBatch(new MultipartFile[0]));
        assertThrows(BaseException.class, () -> mediaService.uploadBatch(null));

        assertNothingStored();
    }

    @Test
    @DisplayName("删除认不出前缀的地址直接报错，不静默跳过")
    void deleteRejectsForeignUrl() {
        BaseException e = assertThrows(BaseException.class,
                () -> mediaService.delete("https://mall-fire.oss-cn-shenzhen.aliyuncs.com/1.jpg"));

        assertEquals(BaseCodeEnum.MEDIA_URL_INVALID.getCode(), e.getCode());
        assertNothingStored();
    }

    @Test
    @DisplayName("删除能把自己拼出来的 URL 还原成 key")
    void deleteResolvesKeyFromUrl() {
        mediaService.delete(CLIENT_POINT + "/" + BUCKET + "/2026/09/21/abc.jpg");

        assertEquals(List.of("2026/09/21/abc.jpg"), storage.removed);
    }

    @Test
    @DisplayName("clientPoint 末尾带斜杠也能正确拼装，不出现双斜杠，且反解析照样认")
    void trailingSlashInClientPointIsNormalized() {
        MediaService service = new MediaServiceImpl(storage, propertiesWith(CLIENT_POINT + "/"));

        MediaFileVo vo = service.upload(new MockMultipartFile("file", "a.png", "image/png", PNG));

        assertTrue(vo.url().startsWith(CLIENT_POINT + "/" + BUCKET + "/"));
        assertFalse(vo.url().contains("//" + BUCKET));

        service.delete(vo.url());
        assertEquals(List.of(storage.puts.get(0).key()), storage.removed);
    }

    @Test
    @DisplayName("批量删除尽力而为：认不出的地址进 failed 清单，其余照删")
    void deleteBatchIsBestEffort() {
        String ours = CLIENT_POINT + "/" + BUCKET + "/2026/09/21/a.jpg";
        String foreign = "https://mall-fire.oss-cn-shenzhen.aliyuncs.com/1.jpg";

        List<String> failed = mediaService.deleteBatch(List.of(ours, foreign));

        assertEquals(List.of(foreign), failed);
        assertEquals(List.of("2026/09/21/a.jpg"), storage.removed);
    }

    @Test
    @DisplayName("批量删除里单个存储失败不中断，其余文件照删")
    void deleteBatchKeepsGoingAfterStorageError() {
        String first = CLIENT_POINT + "/" + BUCKET + "/2026/09/21/a.jpg";
        String second = CLIENT_POINT + "/" + BUCKET + "/2026/09/21/b.jpg";
        storage.failRemoveOf("2026/09/21/a.jpg");

        List<String> failed = mediaService.deleteBatch(List.of(first, second));

        assertEquals(List.of(first), failed);
        // 第一个失败了，第二个仍然被删掉 —— 这才是"不中断"
        assertEquals(List.of("2026/09/21/b.jpg"), storage.removed);
    }

    @Test
    @DisplayName("批量删除空入参直接返回空清单，不碰存储")
    void deleteBatchWithEmptyInput() {
        assertTrue(mediaService.deleteBatch(List.of()).isEmpty());
        assertTrue(mediaService.deleteBatch(null).isEmpty());
        assertNothingStored();
    }
}
