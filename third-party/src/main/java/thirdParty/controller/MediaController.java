package thirdParty.controller;

import common.utils.R;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import thirdParty.service.MediaService;
import thirdParty.vo.MediaFileVo;

import java.util.List;

/**
 * 媒体文件接口。
 *
 * <p>改造前这里是 7 个预签名端点（{@code put / get / delete / batch / validate /
 * expiry / deleteFile}），全部是"给某个 bucket + object 签一个有 TTL 的 URL"。
 * 三个后果：</p>
 *
 * <ol>
 *   <li>bucket 名和完整 object key 由前端决定，后端收到什么签什么；</li>
 *   <li>前端拿到预签名 GET URL 之后<b>直接存进数据库</b>，而预签名默认只有 7 天有效期 ——
 *       到期后全站图片 403，并且 object key 没被任何地方保留，事后既不能重新签发、
 *       也不能干净地删除；</li>
 *   <li>{@code /validate} 会拿调用方给的任意 URL 去发 HEAD 请求（SSRF 入口），
 *       {@code /expiry} 读的是 {@code X-Amz-Expires} 这个"有效期秒数"再加当前时间，
 *       对一个几天前生成的 URL 会返回一个错误的未来时刻。</li>
 * </ol>
 *
 * <p>现在服务端负责 key 生成、类型校验和 URL 拼装，调用方只提交文件、拿回一个永久地址。</p>
 */
@RestController
@RequestMapping("/thirdParty/file")
public class MediaController {

    private final MediaService mediaService;

    public MediaController(MediaService mediaService) {
        this.mediaService = mediaService;
    }

    /** 单文件上传 */
    @PostMapping("/upload")
    public R upload(@RequestParam("file") MultipartFile file) {
        MediaFileVo vo = mediaService.upload(file);
        return R.ok().put("url", vo.url()).put("name", vo.name()).put("size", vo.size());
    }

    /** 批量上传。整批成功或整批失败 */
    @PostMapping("/uploadBatch")
    public R uploadBatch(@RequestParam("files") MultipartFile[] files) {
        return R.ok().put("files", mediaService.uploadBatch(files));
    }

    /** 删除单个文件 */
    @DeleteMapping("/delete")
    public R delete(@RequestParam("url") String url) {
        mediaService.delete(url);
        return R.ok();
    }

    /** 批量删除。尽力而为，删不掉的回 failed 清单 */
    @DeleteMapping("/deleteBatch")
    public R deleteBatch(@RequestBody List<String> urls) {
        return R.ok().put("failed", mediaService.deleteBatch(urls));
    }
}
