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
 *
 * <p>这里提供上传与删除，挂在 {@code /thirdParty/file} 下；会员侧上传见
 * {@code web.MediaFrontController}。
 */
@RestController
@RequestMapping("/thirdParty/file")
public class MediaController {

    private final MediaService mediaService;

    public MediaController(MediaService mediaService) {
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

    /**
     * 删除单个文件。
     *
     * @param url 业务表里存的完整地址，必须由本服务签发
     * @throws common.exception.BaseException 地址解析不出 object key 时抛出
     */
    @DeleteMapping("/delete")
    public R<Void> delete(@RequestParam("url") String url) {
        mediaService.delete(url);
        return R.ok();
    }

    /**
     * 批量删除文件，尽力而为：单个失败不中断。
     *
     * @param urls 待删除的完整地址列表，可为 {@code null}
     * @return 没能删掉的地址，全部成功时是空列表
     */
    @DeleteMapping("/deleteBatch")
    public R<List<String>> deleteBatch(@RequestBody List<String> urls) {
        return R.ok(mediaService.deleteBatch(urls));
    }
}
