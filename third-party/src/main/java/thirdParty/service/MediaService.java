package thirdParty.service;

import org.springframework.web.multipart.MultipartFile;
import thirdParty.vo.MediaFileVo;

import java.util.List;

/**
 * 媒体文件业务接口：上传、删除与地址解析。
 *
 * <p>调用方只提交文件内容，bucket 与 object key 由实现生成，返回的永久地址可直接存库；
 * 实现不得采信调用方给出的路径或 Content-Type。</p>
 */
public interface MediaService {

    /**
     * 上传单个文件。
     *
     * @param file 待上传文件，不能为 {@code null} 或空文件
     * @return 上传结果，含完整可访问地址、原始文件名与字节数
     * @throws common.exception.BaseException 空文件 19000 / 格式不支持 19001 / 超出大小 19002
     */
    MediaFileVo upload(MultipartFile file);

    /**
     * 批量上传，<b>整批原子</b>：先把每个文件都读完并按真实格式校验，全部通过才写对象。
     *
     * <p>半途失败会让调用方拿到"N 个成功 M 个失败"的中间态，而它本来就是一次表单提交；
     * 而且已经写进去的那部分会变成桶里没人引用的垃圾。</p>
     *
     * @param files 待上传文件数组，不能为空
     * @return 上传结果列表，顺序与入参一致
     * @throws common.exception.BaseException 入参为空 19000，其余同 {@link #upload}
     */
    List<MediaFileVo> uploadBatch(MultipartFile[] files);

    /**
     * 删除单个文件。
     *
     * <p>地址不是本服务签发时直接报错，不静默跳过：静默返回会让调用方以为删除成功，
     * 而对象仍留在桶里。</p>
     *
     * @param url 业务表里存的完整地址
     * @throws common.exception.BaseException 地址不合法 19003
     */
    void delete(String url);

    /**
     * 批量删除，<b>尽力而为</b>：单个失败不中断，删不掉的回到返回值里。
     *
     * <p>和 {@link #uploadBatch} 的原子语义刻意不同：删除是清理动作，库里可能残留认不出的地址，
     * 不该因为其中一条对不上就让整批失败。</p>
     *
     * @param urls 待删除的完整地址列表，可为 {@code null} 或空集合
     * @return 没能删掉的地址，全部成功时是空集合（不返回 {@code null}）
     */
    List<String> deleteBatch(List<String> urls);
}
