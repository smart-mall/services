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
 * 媒体文件接口：key 生成、类型校验、URL 拼装都在服务端，调用方只提交文件、拿回永久地址。
 * 这里的是后台管理端的上传和服务间调用的删除，会员侧上传见 {@code web.MediaFrontController}。
 */
@RestController
@RequestMapping("/thirdParty/file")
public class MediaController {

    private final MediaService mediaService;

    public MediaController(MediaService mediaService) {
        this.mediaService = mediaService;
    }

    /** 单文件上传。结果是一条 {@link MediaFileVo}，在 {@code data} 里 */
    @PostMapping("/upload")
    public R<MediaFileVo> upload(@RequestParam("file") MultipartFile file) {
        return R.ok(mediaService.upload(file));
    }

    /** 批量上传。整批成功或整批失败，{@code data} 是 {@link MediaFileVo} 数组，顺序同入参 */
    @PostMapping("/uploadBatch")
    public R<List<MediaFileVo>> uploadBatch(@RequestParam("files") MultipartFile[] files) {
        return R.ok(mediaService.uploadBatch(files));
    }

    /** 删除单个文件 */
    @DeleteMapping("/delete")
    public R<Void> delete(@RequestParam("url") String url) {
        mediaService.delete(url);
        return R.ok();
    }

    /** 批量删除。尽力而为，{@code data} 是删不掉的那些 URL，全成功时是空数组 */
    @DeleteMapping("/deleteBatch")
    public R<List<String>> deleteBatch(@RequestBody List<String> urls) {
        return R.ok(mediaService.deleteBatch(urls));
    }
}
