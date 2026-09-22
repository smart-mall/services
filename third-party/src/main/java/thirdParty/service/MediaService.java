package thirdParty.service;

import org.springframework.web.multipart.MultipartFile;
import thirdParty.vo.MediaFileVo;

import java.util.List;

/**
 * 媒体文件业务接口。
 *
 * <p>改造前这个分工是反的 —— bucket 名和完整 object key 都由前端 {@code new Date()} +
 * {@code getUUID()} + 原始文件名拼好传进来，后端只负责签名（{@code MinIOController}
 * 收到什么 objectName 就签什么），于是"文件存在哪"这件事<b>后端从来没有产出过、
 * 也从来没有校验过</b>。现在反过来：调用方只提交文件内容，路径由实现决定。</p>
 */
public interface MediaService {

    /**
     * 单文件上传。
     *
     * @return 完整可访问地址、原始文件名、字节数
     * @throws common.exception.BaseException 空文件 19000 / 格式不支持 19001 / 超出大小 19002
     */
    MediaFileVo upload(MultipartFile file);

    /**
     * 批量上传，<b>整批原子</b>：先把每个文件都读完并按真实格式校验，全部通过才写对象。
     *
     * <p>半途失败会让调用方拿到"N 个成功 M 个失败"的中间态，而它本来就是一次表单提交；
     * 而且已经写进去的那部分会变成桶里没人引用的垃圾。</p>
     *
     * @throws common.exception.BaseException 入参为空 19000，其余同 {@link #upload}
     */
    List<MediaFileVo> uploadBatch(MultipartFile[] files);

    /**
     * 删除单个文件。
     *
     * <p>地址不是本服务签发的就直接报错，不静默跳过 —— 改造前的实现是反解析失败后
     * 兜底成 {@code bucket="gulimall", objectName=""}，然后 statObject 失败、静默 return，
     * 表现就是"点了删除没反应"，无从下手。</p>
     *
     * @param url 业务表里存的完整地址
     * @throws common.exception.BaseException 地址不合法 19003
     */
    void delete(String url);

    /**
     * 批量删除，<b>尽力而为</b>：单个失败不中断，删不掉的回到返回值里。
     *
     * <p>和 {@link #uploadBatch} 的原子语义刻意不同：删除是清理动作，库里可能残留
     * 认不出的历史地址，不该因为其中一条对不上就让整批失败。</p>
     *
     * @return 没能删掉的地址，全部成功时是空集合（<b>不返回 null</b>）
     */
    List<String> deleteBatch(List<String> urls);
}
