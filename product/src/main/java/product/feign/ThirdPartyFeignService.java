package product.feign;

import common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;


@FeignClient("third-party")
public interface ThirdPartyFeignService {

    /**
     * 批量删除文件。传的是业务表里存的那个完整 URL（不是 object key）。
     *
     * <p>方法名保留 {@code deleteFile} 是为了不动 6 个调用点，语义上它一直是批量的
     * （入参就是 List）。third-party 那边只认自己 {@code clientPoint/bucket/} 前缀下的地址，
     * 对不上的会进返回体的 {@code data} 清单，不会让整个调用失败。</p>
     */
    @DeleteMapping("/thirdParty/file/deleteBatch")
    R deleteFile(@RequestBody List<String> urls);
}
