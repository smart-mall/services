package thirdParty.web;

import common.utils.R;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import thirdParty.service.MediaService;
import thirdParty.vo.MediaFileVo;

import java.util.List;
/** 会员侧的文件上传（头像）。后台管理端用的是 {@code controller.MediaController} 里那两个同路径接口。 */
@RestController
@RequestMapping("thirdParty/front/jwt/file")
public class MediaFrontController {

    private final MediaService mediaService;

    public MediaFrontController(MediaService mediaService) {
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
}
