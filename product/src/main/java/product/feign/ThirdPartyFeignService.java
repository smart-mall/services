package product.feign;

import common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;


/**
 * third-party 服务的 Feign 客户端：删除 MinIO 里的文件。
 */
@FeignClient("third-party")
public interface ThirdPartyFeignService {

    /**
     * 批量删除文件，传的是业务表里存的那个完整 URL（不是 object key）。
     *
     * <p>方法名是单数但语义是批量的，入参与返回都是列表。third-party 只认自己
     * {@code clientPoint/bucket/} 前缀下的地址，对不上的会进返回体的 {@code data} 清单，不会让整个
     * 调用失败；调用方必须检查 {@code data} 是否为空，非空说明这些对象还留在桶里。
     *
     * @param urls 待删除的完整地址列表，允许为 {@code null} 或空集合
     * @return 统一响应体，{@code data} 是没能删掉的地址，全部成功时为空集合；调用失败时 {@code data}
     *         为 {@code null}
     */
    @DeleteMapping("/thirdParty/file/deleteBatch")
    R<List<String>> deleteFile(@RequestBody List<String> urls);
}
