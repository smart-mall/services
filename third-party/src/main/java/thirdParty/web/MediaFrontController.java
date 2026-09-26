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

/**
 * 会员侧文件上传接口（头像），挂在 JWT 鉴权路径下。
 *
 * <p>后台管理端用的是 {@code controller.MediaController} 中两个同路径的接口。
 */
@RestController
@RequestMapping("thirdParty/front/jwt/file")
public class MediaFrontController {

    private final MediaService mediaService;

    public MediaFrontController(MediaService mediaService) {
        this.mediaService = mediaService;
    }

    /**
     * 上传单个文件。
     *
     * @param file 待上传文件，不能为空；按文件头魔数校验，只接受 JPEG、PNG、GIF、WEBP
     * @return 上传结果，{@link MediaFileVo} 放在 {@code data} 中
     * @throws common.exception.BaseException 文件为空、格式不支持或超出大小上限时抛出
     */
    @PostMapping("/upload")
    public R<MediaFileVo> upload(@RequestParam("file") MultipartFile file) {
        return R.ok(mediaService.upload(file));
    }

    /**
     * 批量上传文件，整批原子：全部校验通过后才写入对象存储。
     *
     * @param files 待上传文件数组，不能为空
     * @return 上传结果列表，顺序与入参一致，放在 {@code data} 中
     * @throws common.exception.BaseException 数组为空或任一文件不合法时抛出，此时不会写入任何对象
     */
    @PostMapping("/uploadBatch")
    public R<List<MediaFileVo>> uploadBatch(@RequestParam("files") MultipartFile[] files) {
        return R.ok(mediaService.uploadBatch(files));
    }
}
